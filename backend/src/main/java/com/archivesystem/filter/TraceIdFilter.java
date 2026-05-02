package com.archivesystem.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 过滤器
 * 为每个请求生成唯一的追踪ID，用于日志关联和问题排查
 * @author junyuzhan
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_KEY = "traceId";
    public static final String REQUEST_URI_KEY = "requestUri";
    public static final String USER_ID_KEY = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 优先使用请求头中的traceId，否则生成新的
            String traceId = request.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isEmpty() || !isValidTraceId(traceId)) {
                traceId = generateTraceId();
            }
            
            // 设置到MDC
            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(REQUEST_URI_KEY, request.getRequestURI());
            
            // 设置响应头，便于客户端追踪
            response.setHeader(TRACE_ID_HEADER, traceId);
            
            filterChain.doFilter(request, response);
        } finally {
            // 清理MDC，防止内存泄漏
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(REQUEST_URI_KEY);
            MDC.remove(USER_ID_KEY);
        }
    }
    
    /**
     * 校验客户端传入的TraceId是否合法.
     * 防止日志注入和HTTP Header Injection
     */
    private boolean isValidTraceId(String traceId) {
        if (traceId.length() > 64) {
            return false;
        }
        // 仅允许字母、数字、连字符和下划线
        for (int i = 0; i < traceId.length(); i++) {
            char c = traceId.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_')) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 生成追踪ID
     * 格式: 时间戳(毫秒) + 随机UUID前8位
     */
    private String generateTraceId() {
        return System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 设置用户ID到MDC（供其他组件调用）
     */
    public static void setUserId(Long userId) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, String.valueOf(userId));
        }
    }
}
