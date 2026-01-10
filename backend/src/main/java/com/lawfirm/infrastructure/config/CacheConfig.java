package com.lawfirm.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * 
 * 缓存策略：
 * 1. 一级缓存：Caffeine 本地缓存（高频访问、低延迟）
 * 2. 二级缓存：Redis 分布式缓存（数据一致性、共享）
 * 
 * 使用示例：
 * - @Cacheable(cacheManager = "redisCacheManager", value = "dict", key = "#type")
 * - @Cacheable(cacheManager = "caffeineCacheManager", value = "localCache", key = "#id")
 * 
 * @author Kiro-1
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    // ==================== 缓存名称常量 ====================
    
    /** 字典缓存：TTL 1小时 */
    public static final String CACHE_DICT = "dict";
    
    /** 菜单缓存：TTL 30分钟 */
    public static final String CACHE_MENU = "menu";
    
    /** 配置缓存：TTL 1小时 */
    public static final String CACHE_CONFIG = "config";
    
    /** 部门缓存：TTL 30分钟 */
    public static final String CACHE_DEPARTMENT = "department";
    
    /** 用户缓存：TTL 10分钟 */
    public static final String CACHE_USER = "user";
    
    /** 权限缓存：TTL 30分钟 */
    public static final String CACHE_PERMISSION = "permission";
    
    /** 本地缓存：TTL 5分钟 */
    public static final String CACHE_LOCAL = "localCache";

    // ==================== Redis 缓存配置 ====================
    
    /**
     * Redis 缓存管理器（默认）
     * 
     * 适用场景：
     * - 需要分布式共享的数据
     * - 数据一致性要求高
     * - 跨服务访问
     */
    @Bean
    @Primary
    @ConditionalOnClass(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // 默认 30 分钟
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();  // 不缓存 null 值

        // 不同缓存的独立配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 字典缓存：1小时（低频变更）
        cacheConfigurations.put(CACHE_DICT, defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 菜单缓存：30分钟
        cacheConfigurations.put(CACHE_MENU, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 配置缓存：1小时
        cacheConfigurations.put(CACHE_CONFIG, defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 部门缓存：30分钟
        cacheConfigurations.put(CACHE_DEPARTMENT, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 用户缓存：10分钟（较频繁变更）
        cacheConfigurations.put(CACHE_USER, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 权限缓存：30分钟
        cacheConfigurations.put(CACHE_PERMISSION, defaultConfig.entryTtl(Duration.ofMinutes(30)));

        log.info("Redis 缓存管理器初始化完成，配置 {} 个缓存", cacheConfigurations.size());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()  // 事务感知
                .build();
    }

    // ==================== Caffeine 本地缓存配置 ====================
    
    /**
     * Caffeine 本地缓存管理器
     * 
     * 适用场景：
     * - 高频访问的热点数据
     * - 对延迟敏感的场景
     * - 单机内有效的数据
     */
    @Bean("caffeineCacheManager")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)            // 初始容量
                .maximumSize(1000)               // 最大条目数
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 写入后 5 分钟过期
                .expireAfterAccess(3, TimeUnit.MINUTES) // 访问后 3 分钟过期
                .recordStats());                 // 启用统计
        
        // 允许缓存 null 值（用于防止缓存穿透）
        cacheManager.setAllowNullValues(true);
        
        log.info("Caffeine 本地缓存管理器初始化完成");
        
        return cacheManager;
    }
}

