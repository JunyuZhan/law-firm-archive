package com.archivesystem.security;

import com.archivesystem.common.util.ClientIpUtils;
import com.archivesystem.entity.ExternalSource;
import com.archivesystem.repository.ExternalSourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * API Key认证过滤器.
 * 用于保护开放API接口，从数据库 arc_external_source 表验证 API Key
 * @author junyuzhan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ExternalSourceMapper externalSourceMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${security.api-key.enabled:true}")
    private boolean apiKeyEnabled = true;

    @Value("${security.api-key.header:X-API-Key}")
    private String apiKeyHeader = "X-API-Key";

    // 需要API Key认证的路径模式
    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/open/**",
            "/open/**"
    );

    // 白名单路径（不需要API Key）
    private static final Set<String> WHITELIST_PATHS = Set.of(
            "/api/open/health",
            "/open/health",
            "/api/open/borrow/access/**",
            "/open/borrow/access/**"
    );

    // Redis缓存前缀和过期时间
    private static final String API_KEY_CACHE_PREFIX = "api_key_cache:";
    private static final String API_KEY_USAGE_PREFIX = "api_key_usage:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

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

        // 验证API Key（先从缓存查，缓存未命中再查数据库）
        ExternalSource source = validateApiKey(apiKey);
        if (source == null) {
            log.warn("无效的API Key: uri={}, ip={}", requestUri, getClientIp(request));
            sendUnauthorizedResponse(response, "无效的API Key或来源未启用");
            return;
        }

        // 将来源信息放入请求属性，供后续使用
        request.setAttribute("externalSource", source);
        
        // 记录API调用
        recordApiKeyUsage(apiKey, source.getSourceCode(), requestUri);
        
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
     * 缓存仅用于快速排除无效Key（INVALID标记），有效Key仍需查数据库获取完整对象
     * 
     * @param apiKey API密钥（明文，来自请求头）
     * @return 如果有效返回 ExternalSource，否则返回 null
     */
    private ExternalSource validateApiKey(String apiKey) {
        String apiKeyHash = hashApiKey(apiKey);
        String cacheKey = API_KEY_CACHE_PREFIX + apiKeyHash;

        // 快速排除：如果缓存标记为无效，直接返回null
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if ("INVALID".equals(cached)) {
                return null;
            }
        } catch (Exception e) {
            // Redis不可用时不能放行，否则构成认证绕过；降级为直接查数据库
            log.warn("读取API Key缓存失败，降级数据库校验: {}", e.getMessage());
        }

        // 查询数据库：比对哈希值而非明文
        ExternalSource source = externalSourceMapper.selectOne(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getApiKey, apiKeyHash)
                        .eq(ExternalSource::getEnabled, true)
                        .eq(ExternalSource::getDeleted, false));
        
        if (source != null) {
            return source;
        } else {
            // 缓存无效Key标记，避免重复查库
            try {
                redisTemplate.opsForValue().set(cacheKey, "INVALID", CACHE_TTL);
            } catch (Exception e) {
                log.warn("写入无效API Key缓存失败，忽略: {}", e.getMessage());
            }
            return null;
        }
    }

    /**
     * 对API Key进行SHA-256哈希.
     * 数据库中存储的是哈希值，验证时对请求中的明文Key做哈希后比对
     */
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 记录API Key使用情况.
     */
    private void recordApiKeyUsage(String apiKey, String sourceCode, String uri) {
        try {
            String key = API_KEY_USAGE_PREFIX + sourceCode + ":" + java.time.LocalDate.now();
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
        return ClientIpUtils.resolve(request);
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

    /**
     * 生成新的API Key（明文）.
     * 调用方需自行调用 hashApiKey() 获取哈希值后存入数据库
     * 
     * @return 32位随机字符串（明文）
     */
    public String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 对API Key进行SHA-256哈希（公开方法，供SourceController调用）.
     */
    public String hashApiKeyPublic(String apiKey) {
        return hashApiKey(apiKey);
    }

    /**
     * 清除API Key缓存.
     * 当来源配置更新时调用（传入数据库中存储的哈希值）
     */
    public void clearApiKeyCache(String apiKeyHash) {
        if (apiKeyHash != null) {
            redisTemplate.delete(API_KEY_CACHE_PREFIX + apiKeyHash);
        }
    }
}
