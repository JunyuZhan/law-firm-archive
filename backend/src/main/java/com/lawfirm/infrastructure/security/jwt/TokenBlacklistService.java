package com.lawfirm.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 
 * 功能：
 * 1. 管理已失效的 Token（登出、刷新后的旧 Token）
 * 2. 支持 Token 轮换机制
 * 3. 使用 Redis 存储，自动过期
 * 
 * 安全说明：
 * - 当用户登出时，将 access token 和 refresh token 加入黑名单
 * - 当刷新 token 时，将旧的 refresh token 加入黑名单（防止重放攻击）
 * - 黑名单条目会在 token 原本的过期时间后自动删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Token 黑名单 Key 前缀
     */
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    /**
     * Refresh Token 使用记录前缀（用于防止重放攻击）
     */
    private static final String REFRESH_TOKEN_USED_PREFIX = "token:refresh:used:";

    /**
     * 将 Token 加入黑名单
     *
     * @param token          要加入黑名单的 token
     * @param expireSeconds  黑名单条目过期时间（应该与 token 原过期时间一致）
     */
    public void addToBlacklist(String token, long expireSeconds) {
        if (token == null || token.isEmpty()) {
            return;
        }
        
        String key = BLACKLIST_PREFIX + hashToken(token);
        redisTemplate.opsForValue().set(key, "1", expireSeconds, TimeUnit.SECONDS);
        log.debug("Token 已加入黑名单，过期时间: {}秒", expireSeconds);
    }

    /**
     * 将 Token 加入黑名单（使用默认过期时间 24 小时）
     *
     * @param token 要加入黑名单的 token
     */
    public void addToBlacklist(String token) {
        addToBlacklist(token, 86400); // 24 小时
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param token 要检查的 token
     * @return true 如果在黑名单中
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        String key = BLACKLIST_PREFIX + hashToken(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 标记 Refresh Token 已使用（用于一次性使用检查）
     *
     * @param refreshToken  refresh token
     * @param expireSeconds 过期时间
     * @return true 如果标记成功（首次使用），false 如果已经被使用过
     */
    public boolean markRefreshTokenUsed(String refreshToken, long expireSeconds) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }
        
        String key = REFRESH_TOKEN_USED_PREFIX + hashToken(refreshToken);
        
        // 使用 SETNX（SET if Not eXists）确保原子性
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);
        
        if (Boolean.TRUE.equals(success)) {
            log.debug("Refresh Token 标记为已使用");
            return true;
        } else {
            log.warn("检测到 Refresh Token 重放攻击！");
            return false;
        }
    }

    /**
     * 检查 Refresh Token 是否已被使用
     *
     * @param refreshToken refresh token
     * @return true 如果已被使用
     */
    public boolean isRefreshTokenUsed(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }
        
        String key = REFRESH_TOKEN_USED_PREFIX + hashToken(refreshToken);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 清除用户的所有 Token（用于密码修改等场景）
     * 
     * @param userId 用户ID
     */
    public void invalidateUserTokens(Long userId) {
        // 这里使用一个标记来表示该用户的所有 token 在某个时间点之前都无效
        String key = "token:invalidate:user:" + userId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), 7, TimeUnit.DAYS);
        log.info("用户 {} 的所有 Token 已失效", userId);
    }

    /**
     * 检查用户 Token 是否在失效时间之前签发
     *
     * @param userId   用户ID
     * @param issuedAt Token 签发时间戳（毫秒）
     * @return true 如果 Token 在失效时间之前签发（应该拒绝）
     */
    public boolean isTokenInvalidatedByUser(Long userId, long issuedAt) {
        String key = "token:invalidate:user:" + userId;
        Object invalidateTime = redisTemplate.opsForValue().get(key);
        
        if (invalidateTime != null) {
            long invalidateTimestamp = Long.parseLong(invalidateTime.toString());
            return issuedAt < invalidateTimestamp;
        }
        
        return false;
    }

    /**
     * 对 Token 进行哈希处理（避免直接存储完整 token）
     * 
     * @param token 原始 token
     * @return token 的哈希值
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // 如果 SHA-256 不可用，使用原始 token（不推荐）
            log.warn("SHA-256 不可用，使用原始 token 作为 key");
            return token;
        }
    }
}
