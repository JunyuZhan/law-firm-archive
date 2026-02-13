package com.archivesystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * API Key认证过滤器.
 * 用于保护开放API接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${security.api-key.enabled:true}")
    private boolean apiKeyEnabled;

    @Value("${security.api-key.header:X-API-Key}")
    private String apiKeyHeader;

    // 需要API Key认证的路径模式
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/open/**",
            "/open/**"
    );

    // 白名单路径（不需要API Key）
    private static final Set<String> WHITELIST_PATHS = Set.of(
            "/api/open/health",
            "/open/health"
    );

    private static final String API_KEY_PREFIX = "api_key:";
    private static final String API_KEY_USAGE_PREFIX = "api_key_usage:";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        
        // 检查是否需要API Key认证
        if (!apiKeyEnabled || !isProtectedPath(requestUri) || isWhitelistPath(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(apiKeyHeader);
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("缺少API Key: uri={}, ip={}", requestUri, getClientIp(request));
            sendUnauthorizedResponse(response, "缺少API Key，请在请求头中提供 " + apiKeyHeader);
            return;
        }

        // 验证API Key
        if (!validateApiKey(apiKey)) {
            log.warn("无效的API Key: uri={}, ip={}", requestUri, getClientIp(request));
            sendUnauthorizedResponse(response, "无效的API Key");
            return;
        }

        // 记录API调用
        recordApiKeyUsage(apiKey, requestUri);
        
        filterChain.doFilter(request, response);
    }

    /**
     * 检查路径是否受保护.
     */
    private boolean isProtectedPath(String uri) {
        return PROTECTED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    /**
     * 检查路径是否在白名单中.
     */
    private boolean isWhitelistPath(String uri) {
        return WHITELIST_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    /**
     * 验证API Key.
     */
    private boolean validateApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 记录API Key使用情况.
     */
    private void recordApiKeyUsage(String apiKey, String uri) {
        try {
            String key = API_KEY_USAGE_PREFIX + apiKey + ":" + java.time.LocalDate.now();
            redisTemplate.opsForHash().increment(key, uri, 1);
            redisTemplate.expire(key, Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("记录API Key使用情况失败", e);
        }
    }

    /**
     * 获取客户端IP.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int index = ip.indexOf(',');
            return index != -1 ? ip.substring(0, index).trim() : ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 发送未授权响应.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("code", "401");
        body.put("message", message);
        body.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    // ===== API Key管理方法 =====

    /**
     * 生成新的API Key.
     */
    public String generateApiKey(String sourceName, String description, long expirationDays) {
        String apiKey = java.util.UUID.randomUUID().toString().replace("-", "");
        String key = API_KEY_PREFIX + apiKey;
        
        Map<String, String> keyInfo = new HashMap<>();
        keyInfo.put("sourceName", sourceName);
        keyInfo.put("description", description);
        keyInfo.put("createdAt", java.time.LocalDateTime.now().toString());
        
        redisTemplate.opsForHash().putAll(key, keyInfo);
        if (expirationDays > 0) {
            redisTemplate.expire(key, Duration.ofDays(expirationDays));
        }
        
        log.info("生成新API Key: source={}, expiration={}天", sourceName, expirationDays);
        return apiKey;
    }

    /**
     * 吊销API Key.
     */
    public void revokeApiKey(String apiKey) {
        String key = API_KEY_PREFIX + apiKey;
        redisTemplate.delete(key);
        log.info("API Key已吊销");
    }
}
