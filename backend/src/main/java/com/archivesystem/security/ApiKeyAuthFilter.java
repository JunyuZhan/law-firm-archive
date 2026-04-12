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
     * 先从Redis缓存查询，缓存未命中再查数据库
     * 
     * @param apiKey API密钥
     * @return 如果有效返回 ExternalSource，否则返回 null
     */
    private ExternalSource validateApiKey(String apiKey) {
        String cacheKey = API_KEY_CACHE_PREFIX + apiKey;

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if ("INVALID".equals(cached)) {
                return null;
            }
        } catch (Exception e) {
            log.warn("读取API Key缓存失败，降级数据库校验: {}", e.getMessage());
        }

        // 查询数据库
        ExternalSource source = externalSourceMapper.selectOne(
                new LambdaQueryWrapper<ExternalSource>()
                        .eq(ExternalSource::getApiKey, apiKey)
                        .eq(ExternalSource::getEnabled, true)
                        .eq(ExternalSource::getDeleted, false));
        
        if (source != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, source.getSourceCode(), CACHE_TTL);
            } catch (Exception e) {
                log.warn("写入API Key缓存失败，忽略: {}", e.getMessage());
            }
            return source;
        } else {
            try {
                redisTemplate.opsForValue().set(cacheKey, "INVALID", CACHE_TTL);
            } catch (Exception e) {
                log.warn("写入无效API Key缓存失败，忽略: {}", e.getMessage());
            }
            return null;
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
     * 生成新的API Key.
     * 
     * @return 32位随机字符串
     */
    public String generateApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 清除API Key缓存.
     * 当来源配置更新时调用
     */
    public void clearApiKeyCache(String apiKey) {
        if (apiKey != null) {
            redisTemplate.delete(API_KEY_CACHE_PREFIX + apiKey);
        }
    }
}
