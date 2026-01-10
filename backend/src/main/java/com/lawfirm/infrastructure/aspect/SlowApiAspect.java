package com.lawfirm.infrastructure.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 慢接口监控切面
 * 
 * 功能：
 * 1. 监控所有 Controller 接口执行时间
 * 2. 超过阈值自动告警（记录日志）
 * 3. 统计接口调用情况
 * 
 * @author Kiro-1
 */
@Slf4j
@Aspect
@Component
public class SlowApiAspect {

    /**
     * 慢接口阈值（毫秒），默认 3000ms
     */
    @Value("${law-firm.performance.slow-api-threshold:3000}")
    private long slowApiThreshold;

    /**
     * 警告阈值（毫秒），默认 1000ms
     */
    @Value("${law-firm.performance.warn-api-threshold:1000}")
    private long warnApiThreshold;

    /**
     * 切入点：所有 Controller 的 public 方法
     */
    @Pointcut("execution(public * com.lawfirm.interfaces.rest..*Controller.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：监控接口执行时间
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        String requestUri = "";
        String requestMethod = "";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                requestUri = request.getRequestURI();
                requestMethod = request.getMethod();
            }
        } catch (Exception e) {
            // 忽略获取请求信息的异常
        }

        // 获取方法信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        Object result = null;
        Throwable throwable = null;
        
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            throwable = t;
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            
            // 记录日志
            logApiPerformance(requestMethod, requestUri, className, methodName, 
                    costTime, throwable != null);
        }
    }

    /**
     * 记录 API 性能日志
     */
    private void logApiPerformance(String method, String uri, String className, 
                                   String methodName, long costTime, boolean hasError) {
        String apiInfo = String.format("%s %s [%s.%s]", method, uri, className, methodName);
        
        if (hasError) {
            log.error("❌ 接口异常 - {} - {}ms", apiInfo, costTime);
        } else if (costTime >= slowApiThreshold) {
            // 慢接口告警
            log.error("🐢 慢接口告警 - {} - {}ms (阈值: {}ms)", apiInfo, costTime, slowApiThreshold);
            // TODO: 可以在这里添加告警通知（邮件、钉钉、企业微信等）
        } else if (costTime >= warnApiThreshold) {
            // 性能警告
            log.warn("⚠️ 接口较慢 - {} - {}ms", apiInfo, costTime);
        } else {
            // 正常日志（DEBUG 级别，生产环境可关闭）
            if (log.isDebugEnabled()) {
                log.debug("✅ 接口正常 - {} - {}ms", apiInfo, costTime);
            }
        }
    }
}

