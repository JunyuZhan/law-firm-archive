package com.lawfirm.common.aspect;

import com.lawfirm.common.annotation.RateLimiter;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.IpUtils;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
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

/**
 * 限流切面
 *
 * <p>使用Redis实现分布式限流
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

  /** Redis模板 */
  private final RedisTemplate<String, Object> redisTemplate;

  /** 限流键前缀 */
  private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

  /** SpEL表达式解析器 */
  private static final SpelExpressionParser PARSER = new SpelExpressionParser();

  /** 参数名发现器 */
  private static final DefaultParameterNameDiscoverer NAME_DISCOVERER =
      new DefaultParameterNameDiscoverer();

  /**
   * 限流切面方法
   *
   * @param point 连接点
   * @param rateLimiter 限流注解
   * @return 方法执行结果
   * @throws Throwable 异常
   */
  @Around("@annotation(rateLimiter)")
  public Object around(final ProceedingJoinPoint point, final RateLimiter rateLimiter)
      throws Throwable {
    // 1. 构建限流键
    String key = buildKey(point, rateLimiter);

    // 2. 获取限流参数
    int rate = rateLimiter.rate();
    int interval = rateLimiter.interval();

    // 3. 检查是否超过限制
    String redisKey = RATE_LIMIT_KEY_PREFIX + key;
    Long count = redisTemplate.opsForValue().increment(redisKey, 1);

    if (count == null) {
      count = 1L;
    }

    // 首次访问，设置过期时间
    if (count == 1) {
      redisTemplate.expire(redisKey, interval, TimeUnit.SECONDS);
    }

    // 4. 判断是否超过限制
    if (count > rate) {
      log.warn("Rate limit exceeded: key={}, count={}, rate={}", key, count, rate);
      throw new BusinessException(rateLimiter.message());
    }

    // 5. 执行原方法
    return point.proceed();
  }

  /**
   * 构建限流键
   *
   * @param point 连接点
   * @param rateLimiter 限流注解
   * @return 限流键
   */
  private String buildKey(final ProceedingJoinPoint point, final RateLimiter rateLimiter) {
    MethodSignature signature = (MethodSignature) point.getSignature();
    Method method = signature.getMethod();

    // 根据限流类型构建基础键
    String baseKey =
        switch (rateLimiter.limitType()) {
          case IP -> "ip:" + getClientIp();
          case USER -> "user:" + getCurrentUserId();
          case GLOBAL -> "global";
          default -> method.getDeclaringClass().getSimpleName() + ":" + method.getName();
        };

    // 如果指定了自定义key，使用SpEL解析
    String customKey = rateLimiter.key();
    if (customKey != null && !customKey.isEmpty()) {
      String parsedKey = parseSpelExpression(customKey, method, point.getArgs());
      return baseKey + ":" + parsedKey;
    }

    return baseKey;
  }

  /**
   * 解析SpEL表达式
   *
   * @param expressionString 表达式字符串
   * @param method 方法
   * @param args 参数
   * @return 解析结果
   */
  private String parseSpelExpression(
      final String expressionString, final Method method, final Object[] args) {
    try {
      String[] paramNames = NAME_DISCOVERER.getParameterNames(method);
      if (paramNames == null || paramNames.length == 0) {
        return expressionString;
      }

      EvaluationContext context = new StandardEvaluationContext();
      for (int i = 0; i < paramNames.length && i < args.length; i++) {
        ((StandardEvaluationContext) context).setVariable(paramNames[i], args[i]);
      }

      Expression expression = PARSER.parseExpression(expressionString);
      Object value = expression.getValue(context);
      return value != null ? value.toString() : expressionString;
    } catch (Exception e) {
      log.warn("Failed to parse SpEL expression: {}", expressionString, e);
      return expressionString;
    }
  }

  /**
   * 获取客户端IP
   *
   * @return 客户端IP地址，获取失败返回"unknown"
   */
  private String getClientIp() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        HttpServletRequest request = attributes.getRequest();
        return IpUtils.getIpAddr(request);
      }
    } catch (Exception e) {
      log.debug("Failed to get client IP", e);
    }
    return "unknown";
  }

  /**
   * 获取当前用户ID
   *
   * @return 当前用户ID，获取失败返回"anonymous"
   */
  private String getCurrentUserId() {
    try {
      Long userId = SecurityUtils.getUserId();
      return userId != null ? userId.toString() : "anonymous";
    } catch (Exception e) {
      return "anonymous";
    }
  }
}
