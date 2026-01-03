package com.lawfirm.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ObjectMapper objectMapper;

    @Around("@annotation(com.lawfirm.common.annotation.OperationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 1. 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        // 2. 获取请求信息
        HttpServletRequest request = getRequest();
        String requestUri = request != null ? request.getRequestURI() : "";
        String requestMethod = request != null ? request.getMethod() : "";
        String ip = request != null ? getClientIp(request) : "";

        // 3. 获取用户信息
        Long userId = null;
        String username = "anonymous";
        try {
            userId = SecurityUtils.getUserId();
            username = SecurityUtils.getUsername();
        } catch (Exception ignored) {
            // 未登录状态
        }

        // 4. 记录请求参数
        String params = "";
        if (annotation.saveParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                params = objectMapper.writeValueAsString(args);
                if (params.length() > 2000) {
                    params = params.substring(0, 2000) + "...";
                }
            } catch (Exception e) {
                params = "参数序列化失败";
            }
        }

        // 5. 执行方法
        Object result = null;
        String errorMsg = null;
        boolean success = true;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;

            // 6. 记录日志
            log.info("操作日志 | 模块:{} | 操作:{} | 用户:{}({}) | IP:{} | URI:{} {} | 耗时:{}ms | 成功:{} | 错误:{}",
                    annotation.module(),
                    annotation.action(),
                    username,
                    userId,
                    ip,
                    requestMethod,
                    requestUri,
                    costTime,
                    success,
                    errorMsg
            );

            // TODO: 保存到数据库
            // operationLogRepository.save(...)
        }

        return result;
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

