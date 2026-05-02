package com.archivesystem.security;

import com.archivesystem.common.util.ClientIpUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 速率限制过滤器.
 * 防止暴力攻击和DoS攻击
 * @author junyuzhan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Lua脚本：原子increment + 条件expire（仅在key首次创建时设置TTL）
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return count",
            Long.class
    );

    // 通用接口限制：每分钟100次请求
    private static final int GENERAL_RATE_LIMIT = 100;
    private static final int GENERAL_WINDOW_SECONDS = 60;

    // 登录接口限制：每分钟10次
    private static final int LOGIN_RATE_LIMIT = 10;
    private static final int LOGIN_WINDOW_SECONDS = 60;

    // 文件上传限制：每分钟20次
    private static final int UPLOAD_RATE_LIMIT = 20;
    private static final int UPLOAD_WINDOW_SECONDS = 60;

    // 公开借阅访问限制：每分钟12次
    private static final int PUBLIC_BORROW_ACCESS_RATE_LIMIT = 12;
    private static final int PUBLIC_BORROW_ACCESS_WINDOW_SECONDS = 60;

    // 开放写接口限制：每分钟20次
    private static final int OPEN_API_WRITE_RATE_LIMIT = 20;
    private static final int OPEN_API_WRITE_WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = ClientIpUtils.resolve(request);
        String requestUri = request.getRequestURI();
        
        // 确定适用的速率限制
        RateLimitConfig config = getRateLimitConfig(requestUri);
        
        String key = "rate_limit:" + config.keyPrefix + ":" + clientIp;
        
        try {
            // 使用Lua脚本原子执行increment + 条件expire，避免竞态条件
            Long count = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(config.windowSeconds)
            );
            
            // 设置速率限制响应头
            response.setHeader("X-RateLimit-Limit", String.valueOf(config.limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, config.limit - (count != null ? count : 0))));
            
            if (count != null && count > config.limit) {
                log.warn("速率限制触发: IP={}, URI={}, Count={}", clientIp, requestUri, count);
                sendRateLimitResponse(response, config.limit, config.windowSeconds);
                return;
            }
        } catch (Exception e) {
            // Redis异常时不阻止请求，但记录日志
            log.error("速率限制检查失败", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 根据请求URI获取速率限制配置.
     * 使用AntPathMatcher精确匹配，避免contains()被绕过
     */
    private RateLimitConfig getRateLimitConfig(String uri) {
        if (pathMatcher.match("/api/auth/login", uri) || pathMatcher.match("/auth/login", uri)) {
            return new RateLimitConfig("login", LOGIN_RATE_LIMIT, LOGIN_WINDOW_SECONDS);
        } else if (pathMatcher.match("/api/files/upload", uri) || pathMatcher.match("/files/upload", uri)) {
            return new RateLimitConfig("upload", UPLOAD_RATE_LIMIT, UPLOAD_WINDOW_SECONDS);
        } else if (pathMatcher.match("/api/open/borrow/access/**", uri) || pathMatcher.match("/open/borrow/access/**", uri)) {
            return new RateLimitConfig("open_borrow_access", PUBLIC_BORROW_ACCESS_RATE_LIMIT, PUBLIC_BORROW_ACCESS_WINDOW_SECONDS);
        } else if (pathMatcher.match("/api/open/**", uri) || pathMatcher.match("/open/**", uri)) {
            return new RateLimitConfig("open_write", OPEN_API_WRITE_RATE_LIMIT, OPEN_API_WRITE_WINDOW_SECONDS);
        }
        return new RateLimitConfig("general", GENERAL_RATE_LIMIT, GENERAL_WINDOW_SECONDS);
    }

    /**
     * 发送速率限制响应.
     */
    private void sendRateLimitResponse(HttpServletResponse response, int limit, int windowSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", "429");
        body.put("message", String.format("请求过于频繁，请%d秒后重试。限制：每%d秒%d次请求", 
                windowSeconds, windowSeconds, limit));
        body.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * 速率限制配置.
     */
    private record RateLimitConfig(String keyPrefix, int limit, int windowSeconds) {}
}
