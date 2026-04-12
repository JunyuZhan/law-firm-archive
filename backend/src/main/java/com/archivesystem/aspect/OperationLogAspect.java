package com.archivesystem.aspect;

import com.archivesystem.common.util.ClientIpUtils;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面.
 * 自动记录带有 @Log 注解的方法执行.
 * @author junyuzhan
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Pointcut("@annotation(com.archivesystem.aspect.Log)")
    public void logPointcut() {}

    @AfterReturning(pointcut = "logPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Log logAnnotation = method.getAnnotation(Log.class);
            
            if (logAnnotation == null) {
                return;
            }
            
            OperationLog operationLog = new OperationLog();
            operationLog.setObjectType(logAnnotation.objectType());
            operationLog.setOperationType(logAnnotation.operationType());
            operationLog.setOperationDesc(logAnnotation.description());
            operationLog.setOperatedAt(LocalDateTime.now());
            
            // 获取参数详情
            Map<String, Object> detail = new HashMap<>();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName();
                Object paramValue = args[i];
                
                // 过滤敏感参数
                if (paramName.toLowerCase().contains("password")) {
                    continue;
                }
                
                // 如果参数是id，设置为objectId
                if (paramName.equals("id") && paramValue != null) {
                    operationLog.setObjectId(paramValue.toString());
                }
                
                // 简单类型直接放入，复杂类型尝试序列化
                if (paramValue != null && isSimpleType(paramValue.getClass())) {
                    detail.put(paramName, paramValue);
                }
            }
            
            if (!detail.isEmpty()) {
                operationLog.setOperationDetail(detail);
            }
            
            // 获取操作人信息
            try {
                operationLog.setOperatorId(SecurityUtils.getCurrentUserId());
                operationLog.setOperatorName(SecurityUtils.getCurrentRealName());
            } catch (Exception ignored) {
            }
            
            // 获取请求信息
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    operationLog.setOperatorIp(getClientIp(request));
                    operationLog.setOperatorUa(request.getHeader("User-Agent"));
                }
            } catch (Exception ignored) {
            }
            
            operationLogService.log(operationLog);
            
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() ||
               type == String.class ||
               Number.class.isAssignableFrom(type) ||
               type == Boolean.class ||
               type.isEnum();
    }

    private String getClientIp(HttpServletRequest request) {
        return ClientIpUtils.resolve(request);
    }
}
