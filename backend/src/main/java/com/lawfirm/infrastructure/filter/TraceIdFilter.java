package com.lawfirm.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求追踪过滤器
 * 
 * 功能：
 * - 生成或传递TraceId
 * - 注入MDC供日志使用
 * - 响应头返回TraceId
 * 
 * 使用方法：
 * 1. 在logback配置中添加 %X{traceId} 输出TraceId
 * 2. 请求可通过 X-Trace-Id 头传入自定义TraceId
 * 3. 响应头中会返回 X-Trace-Id
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    /**
     * TraceId的MDC键名
     */
    public static final String TRACE_ID_KEY = "traceId";

    /**
     * TraceId的Header名称
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 请求开始时间的属性名
     */
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        request.setAttribute(REQUEST_START_TIME, startTime);

        try {
            // 1. 获取或生成TraceId
            String traceId = getOrGenerateTraceId(request);

            // 2. 放入MDC，供日志使用
            MDC.put(TRACE_ID_KEY, traceId);

            // 3. 添加到响应头，方便前端和外部系统追踪
            response.addHeader(TRACE_ID_HEADER, traceId);

            // 4. 记录请求开始日志
            if (log.isDebugEnabled()) {
                log.debug("Request started: {} {} [traceId={}]", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    traceId);
            }

            // 5. 执行过滤链
            filterChain.doFilter(request, response);

        } finally {
            // 6. 记录请求结束日志
            long duration = System.currentTimeMillis() - startTime;
            if (log.isDebugEnabled()) {
                log.debug("Request completed: {} {} - {}ms [status={}]", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    duration,
                    response.getStatus());
            }

            // 7. 慢请求警告（超过3秒）
            if (duration > 3000) {
                log.warn("Slow request detected: {} {} - {}ms", 
                    request.getMethod(), 
                    request.getRequestURI(), 
                    duration);
            }

            // 8. 清理MDC，避免线程复用导致的污染
            MDC.remove(TRACE_ID_KEY);
        }
    }

    /**
     * 获取或生成TraceId
     * 优先从请求头获取，如果没有则生成新的
     */
    private String getOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        
        if (traceId == null || traceId.isEmpty()) {
            // 生成32位无横线的UUID作为TraceId
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        return traceId;
    }

    /**
     * 获取当前请求的TraceId
     * 可在代码任意位置调用获取当前TraceId
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 手动设置TraceId（用于异步任务等场景）
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 清除TraceId（用于异步任务完成后清理）
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
}

