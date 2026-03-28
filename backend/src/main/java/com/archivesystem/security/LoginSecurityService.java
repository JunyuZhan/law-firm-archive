package com.archivesystem.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录安全服务.
 * 提供登录失败锁定、IP限制等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityService {

    private final StringRedisTemplate redisTemplate;

    // 最大登录失败次数
    private static final int MAX_FAILED_ATTEMPTS = 5;
    // 账号锁定时间（分钟）
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    // 失败记录过期时间（分钟）
    private static final int FAILED_ATTEMPTS_EXPIRY_MINUTES = 60;

    private static final String FAILED_ATTEMPTS_KEY_PREFIX = "login:failed:";
    private static final String LOCKOUT_KEY_PREFIX = "login:locked:";
    private static final String IP_FAILED_KEY_PREFIX = "login:ip_failed:";

    /**
     * 检查账号是否被锁定.
     */
    public boolean isAccountLocked(String username) {
        String key = LOCKOUT_KEY_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 检查IP是否被锁定.
     */
    public boolean isIpLocked(String ip) {
        String key = IP_FAILED_KEY_PREFIX + ip;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            // IP在短时间内失败次数过多
            return count >= MAX_FAILED_ATTEMPTS * 3;
        }
        return false;
    }

    /**
     * 获取账号剩余锁定时间（秒）.
     */
    public long getRemainingLockoutTime(String username) {
        String key = LOCKOUT_KEY_PREFIX + username;
        Long ttl = redisTemplate.getExpire(key);
        return ttl != null && ttl > 0 ? ttl : 0;
    }

    /**
     * 记录登录失败.
     */
    public int recordFailedAttempt(String username, String ip) {
        String userKey = FAILED_ATTEMPTS_KEY_PREFIX + username;
        String ipKey = IP_FAILED_KEY_PREFIX + ip;
        
        // 增加用户失败计数
        Long userCount = redisTemplate.opsForValue().increment(userKey);
        if (userCount != null && userCount == 1) {
            redisTemplate.expire(userKey, Duration.ofMinutes(FAILED_ATTEMPTS_EXPIRY_MINUTES));
        }
        
        // 增加IP失败计数
        Long ipCount = redisTemplate.opsForValue().increment(ipKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(ipKey, Duration.ofMinutes(FAILED_ATTEMPTS_EXPIRY_MINUTES));
        }
        
        int failedCount = userCount != null ? userCount.intValue() : 0;
        
        // 检查是否需要锁定账号
        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            lockAccount(username);
            log.warn("账号因多次登录失败被锁定: username={}, attempts={}, ip={}", username, failedCount, ip);
        } else {
            log.warn("登录失败: username={}, attempts={}/{}, ip={}", username, failedCount, MAX_FAILED_ATTEMPTS, ip);
        }
        
        return failedCount;
    }

    /**
     * 锁定账号.
     */
    private void lockAccount(String username) {
        String lockKey = LOCKOUT_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(lockKey, "locked", Duration.ofMinutes(LOCKOUT_DURATION_MINUTES));
    }

    /**
     * 登录成功，清除失败记录.
     */
    public void clearFailedAttempts(String username) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + username;
        redisTemplate.delete(key);
    }

    /**
     * 解锁账号（管理员操作）.
     */
    public void unlockAccount(String username) {
        String lockKey = LOCKOUT_KEY_PREFIX + username;
        String failedKey = FAILED_ATTEMPTS_KEY_PREFIX + username;
        redisTemplate.delete(lockKey);
        redisTemplate.delete(failedKey);
        log.info("账号已解锁: username={}", username);
    }

    /**
     * 获取剩余登录尝试次数.
     */
    public int getRemainingAttempts(String username) {
        String key = FAILED_ATTEMPTS_KEY_PREFIX + username;
        String countStr = redisTemplate.opsForValue().get(key);
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            return Math.max(0, MAX_FAILED_ATTEMPTS - count);
        }
        return MAX_FAILED_ATTEMPTS;
    }
}
