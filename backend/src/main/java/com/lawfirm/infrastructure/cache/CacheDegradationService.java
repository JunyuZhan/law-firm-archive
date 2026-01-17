package com.lawfirm.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lawfirm.common.resilience.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存降级服务
 * 
 * Redis故障时自动降级到本地Caffeine缓存
 * 集成熔断器保护，防止级联故障
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Service
public class CacheDegradationService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 本地缓存（降级使用）
     */
    private final Cache<String, Object> localCache;

    /**
     * Redis 熔断器
     */
    private final CircuitBreaker redisCircuitBreaker;

    /**
     * 检查间隔（毫秒）
     */
    private static final long CHECK_INTERVAL = 30000;

    public CacheDegradationService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.localCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build();
        // 初始化熔断器：5次失败熔断，30秒超时，2次成功恢复
        this.redisCircuitBreaker = CircuitBreaker.of("redis-cache", 5, CHECK_INTERVAL, 2);
    }

    /**
     * 获取缓存（带降级）
     * 
     * @param key 缓存key
     * @param dbLoader 数据库加载器（缓存未命中时调用）
     * @param expireSeconds 过期时间（秒）
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T getWithFallback(String key, Supplier<T> dbLoader, long expireSeconds) {
        // 1. 先检查本地缓存
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            log.debug("本地缓存命中: {}", key);
            return (T) cached;
        }

        // 2. 使用熔断器保护的 Redis 操作
        return redisCircuitBreaker.executeWithFallback(
            () -> {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    localCache.put(key, value);
                    return (T) value;
                }
                
                // Redis未命中，从DB加载
                T data = dbLoader.get();
                if (data != null) {
                    redisTemplate.opsForValue().set(key, data, expireSeconds, TimeUnit.SECONDS);
                    localCache.put(key, data);
                }
                return data;
            },
            () -> {
                // 熔断降级：从本地缓存或DB加载
                log.warn("Redis熔断，降级到本地缓存: {}", key);
                return getFromLocalCache(key, dbLoader);
            }
        );
    }

    /**
     * 设置缓存（带降级）
     */
    public void setWithFallback(String key, Object value, long expireSeconds) {
        // 先写入本地缓存
        localCache.put(key, value);

        // 使用熔断器保护的 Redis 写入
        try {
            redisCircuitBreaker.execute(() -> {
                redisTemplate.opsForValue().set(key, value, expireSeconds, TimeUnit.SECONDS);
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis写入失败，仅保存到本地缓存: {}", e.getMessage());
        }
    }

    /**
     * 删除缓存（带降级）
     */
    public void deleteWithFallback(String key) {
        // 删除本地缓存
        localCache.invalidate(key);

        // 使用熔断器保护的 Redis 删除
        try {
            redisCircuitBreaker.execute(() -> {
                redisTemplate.delete(key);
                return null;
            });
        } catch (Exception e) {
            log.warn("Redis删除失败: {}", e.getMessage());
        }
    }

    /**
     * 从本地缓存获取
     */
    @SuppressWarnings("unchecked")
    private <T> T getFromLocalCache(String key, Supplier<T> dbLoader) {
        Object cached = localCache.getIfPresent(key);
        if (cached != null) {
            log.debug("本地缓存命中: {}", key);
            return (T) cached;
        }

        // 本地缓存未命中，从DB加载
        T data = dbLoader.get();
        if (data != null) {
            localCache.put(key, data);
        }
        return data;
    }

    /**
     * 获取本地缓存统计
     */
    public String getLocalCacheStats() {
        var stats = localCache.stats();
        return String.format(
            "LocalCache[hitCount=%d, missCount=%d, hitRate=%.2f%%, size=%d]",
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate() * 100,
            localCache.estimatedSize()
        );
    }

    /**
     * 获取熔断器状态
     */
    public CircuitBreaker.State getCircuitBreakerState() {
        return redisCircuitBreaker.getState();
    }

    /**
     * 获取熔断器信息
     */
    public String getCircuitBreakerInfo() {
        return redisCircuitBreaker.toString();
    }

    /**
     * 清空本地缓存
     */
    public void clearLocalCache() {
        localCache.invalidateAll();
        log.info("本地缓存已清空");
    }
}
