package com.lawfirm.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.annotation.RepeatSubmit;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 * 
 * 使用Redis实现分布式防重提交
 * 
 * @author system
 * @since 2026-01-10
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REPEAT_SUBMIT_KEY_PREFIX = "repeat_submit:";
    private static final String SUBMIT_TOKEN_HEADER = "X-Submit-Token";
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final DefaultParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) throws Throwable {
        // 1. 构建去重键
        String key = buildKey(point, repeatSubmit);
        String redisKey = REPEAT_SUBMIT_KEY_PREFIX + key;
        
        // 2. 计算过期时间
        long intervalMillis = repeatSubmit.timeUnit().toMillis(repeatSubmit.interval());
        
        // 3. 尝试设置键（如果不存在则设置成功）
        Boolean success = redisTemplate.opsForValue().setIfAbsent(
            redisKey, 
            System.currentTimeMillis(), 
            intervalMillis, 
            TimeUnit.MILLISECONDS
        );
        
        // 4. 判断是否重复提交
        if (success == null || !success) {
            log.warn("Repeat submit detected: key={}", key);
            throw new BusinessException(repeatSubmit.message());
        }
        
        try {
            // 5. 执行原方法
            return point.proceed();
        } catch (Throwable e) {
            // 6. 如果执行失败，删除键允许重试
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    /**
     * 构建去重键
     */
    private String buildKey(ProceedingJoinPoint point, RepeatSubmit repeatSubmit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String methodKey = method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        
        return switch (repeatSubmit.keyStrategy()) {
            case TOKEN -> buildTokenKey(methodKey);
            case PARAMS -> buildParamsKey(methodKey, point.getArgs());
            case USER_METHOD -> buildUserMethodKey(methodKey);
            case CUSTOM -> buildCustomKey(methodKey, repeatSubmit.key(), method, point.getArgs());
        };
    }

    /**
     * 使用Token构建键
     */
    private String buildTokenKey(String methodKey) {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return methodKey + ":unknown";
        }
        
        String token = request.getHeader(SUBMIT_TOKEN_HEADER);
        if (token == null || token.isEmpty()) {
            // 如果没有Token，使用SessionId
            token = request.getSession().getId();
        }
        
        return methodKey + ":" + token;
    }

    /**
     * 使用参数哈希构建键
     */
    private String buildParamsKey(String methodKey, Object[] args) {
        try {
            String paramsJson = objectMapper.writeValueAsString(args);
            String hash = md5(paramsJson);
            return methodKey + ":" + hash;
        } catch (Exception e) {
            log.warn("Failed to build params key", e);
            return methodKey + ":" + System.identityHashCode(args);
        }
    }

    /**
     * 使用用户+方法构建键
     */
    private String buildUserMethodKey(String methodKey) {
        try {
            Long userId = SecurityUtils.getUserId();
            String userKey = userId != null ? userId.toString() : "anonymous";
            return methodKey + ":" + userKey;
        } catch (Exception e) {
            return methodKey + ":anonymous";
        }
    }

    /**
     * 使用自定义SpEL表达式构建键
     */
    private String buildCustomKey(String methodKey, String expression, Method method, Object[] args) {
        if (expression == null || expression.isEmpty()) {
            return methodKey;
        }
        
        try {
            String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
            if (paramNames == null || paramNames.length == 0) {
                return methodKey + ":" + expression;
            }
            
            EvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
            }
            
            Expression exp = PARSER.parseExpression(expression);
            Object value = exp.getValue(context);
            return methodKey + ":" + (value != null ? value.toString() : expression);
        } catch (Exception e) {
            log.warn("Failed to parse custom key expression: {}", expression, e);
            return methodKey + ":" + expression;
        }
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算MD5哈希
     */
    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
