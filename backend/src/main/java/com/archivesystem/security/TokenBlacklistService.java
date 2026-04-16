package com.archivesystem.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

/**
 * Token黑名单服务.
 * 用于实现JWT登出功能
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtils jwtUtils;

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    /**
     * 将Token加入黑名单.
     */
    public void addToBlacklist(String token) {
        try {
            Claims claims = jwtUtils.parseToken(token);
            Date expiration = claims.getExpiration();
            
            // 计算Token剩余有效时间
            long ttlMillis = expiration.getTime() - System.currentTimeMillis();
            if (ttlMillis > 0) {
                String key = BLACKLIST_KEY_PREFIX + getTokenId(token);
                redisTemplate.opsForValue().set(key, "blacklisted", Duration.ofMillis(ttlMillis));
                log.debug("Token已加入黑名单，将在{}毫秒后自动移除", ttlMillis);
            }
        } catch (Exception e) {
            log.warn("将Token加入黑名单失败", e);
        }
    }

    /**
     * 检查Token是否在黑名单中.
     */
    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + getTokenId(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("检查Token黑名单失败", e);
            return false;
        }
    }

    /**
     * 从Token获取唯一标识（使用SHA-256摘要避免碰撞误伤）.
     */
    private String getTokenId(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("生成Token黑名单标识失败", e);
        }
    }

    /**
     * 将用户所有Token加入黑名单（用于强制登出）.
     * 通过设置用户级别的标记实现
     */
    public void blacklistUserTokens(Long userId, long durationSeconds) {
        String key = "token:user_blacklist:" + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), 
                Duration.ofSeconds(durationSeconds));
        log.info("用户{}的所有Token已被加入黑名单", userId);
    }

    /**
     * 检查用户Token是否被全部吊销.
     */
    public boolean isUserBlacklisted(Long userId, long tokenIssuedAt) {
        try {
            String key = "token:user_blacklist:" + userId;
            String blacklistTime = redisTemplate.opsForValue().get(key);
            if (blacklistTime != null) {
                long blacklistTimestamp = Long.parseLong(blacklistTime);
                // 如果Token是在黑名单时间之前签发的，则视为无效
                return tokenIssuedAt < blacklistTimestamp;
            }
            return false;
        } catch (Exception e) {
            log.warn("检查用户Token黑名单失败", e);
            return false;
        }
    }
}
