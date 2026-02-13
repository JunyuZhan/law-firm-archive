package com.lawfirm.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

/**
 * 缓存配置
 *
 * <p>缓存策略： 1. 一级缓存：Caffeine 本地缓存（高频访问、低延迟） 2. 二级缓存：Redis 分布式缓存（数据一致性、共享）
 *
 * <p>使用示例： - @Cacheable(cacheManager = "redisCacheManager", value = "dict", key = "#type")
 * - @Cacheable(cacheManager = "caffeineCacheManager", value = "localCache", key = "#id")
 *
 * @author junyuzhan
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

  /** 默认TTL（分钟）. */
  private static final long DEFAULT_TTL_MINUTES = 30L;

  /** 字典缓存TTL（小时）. */
  private static final long DICT_TTL_HOURS = 1L;

  /** 菜单缓存TTL（分钟）. */
  private static final long MENU_TTL_MINUTES = 30L;

  /** 配置缓存TTL（小时）. */
  private static final long CONFIG_TTL_HOURS = 1L;

  /** 部门缓存TTL（分钟）. */
  private static final long DEPT_TTL_MINUTES = 30L;

  /** 用户缓存TTL（分钟）. */
  private static final long USER_TTL_MINUTES = 10L;

  /** 权限缓存TTL（分钟）. */
  private static final long PERMISSION_TTL_MINUTES = 30L;

  /** Caffeine初始容量. */
  private static final int CAFFEINE_INITIAL_CAPACITY = 100;

  /** Caffeine最大容量. */
  private static final int CAFFEINE_MAXIMUM_SIZE = 1000;

  /** Caffeine写入过期时间（分钟）. */
  private static final int CAFFEINE_EXPIRE_AFTER_WRITE_MINUTES = 5;

  /** Caffeine访问过期时间（分钟）. */
  private static final int CAFFEINE_EXPIRE_AFTER_ACCESS_MINUTES = 3;

  // ==================== Redis 缓存配置 ====================

  /**
   * Redis 缓存管理器（默认）.
   *
   * <p>适用场景： - 需要分布式共享的数据 - 数据一致性要求高 - 跨服务访问
   *
   * @param connectionFactory Redis连接工厂
   * @return 缓存管理器
   */
  @Bean
  @Primary
  @ConditionalOnClass(RedisConnectionFactory.class)
  public CacheManager redisCacheManager(final RedisConnectionFactory connectionFactory) {
    // 默认配置
    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(DEFAULT_TTL_MINUTES)) // 默认 30 分钟
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues(); // 不缓存 null 值

    // 不同缓存的独立配置
    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // 字典缓存：1小时（低频变更）
    cacheConfigurations.put(CACHE_DICT, defaultConfig.entryTtl(Duration.ofHours(DICT_TTL_HOURS)));

    // 菜单缓存：30分钟
    cacheConfigurations.put(
        CACHE_MENU, defaultConfig.entryTtl(Duration.ofMinutes(MENU_TTL_MINUTES)));

    // 配置缓存：1小时
    cacheConfigurations.put(
        CACHE_CONFIG, defaultConfig.entryTtl(Duration.ofHours(CONFIG_TTL_HOURS)));

    // 部门缓存：30分钟
    cacheConfigurations.put(
        CACHE_DEPARTMENT, defaultConfig.entryTtl(Duration.ofMinutes(DEPT_TTL_MINUTES)));

    // 用户缓存：10分钟（较频繁变更）
    cacheConfigurations.put(
        CACHE_USER, defaultConfig.entryTtl(Duration.ofMinutes(USER_TTL_MINUTES)));

    // 权限缓存：30分钟
    cacheConfigurations.put(
        CACHE_PERMISSION, defaultConfig.entryTtl(Duration.ofMinutes(PERMISSION_TTL_MINUTES)));

    log.info("Redis 缓存管理器初始化完成，配置 {} 个缓存", cacheConfigurations.size());

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware() // 事务感知
        .build();
  }

  // ==================== Caffeine 本地缓存配置 ====================

  /**
   * Caffeine 本地缓存管理器.
   *
   * <p>适用场景： - 高频访问的热点数据 - 对延迟敏感的场景 - 单机内有效的数据
   *
   * @return 缓存管理器
   */
  @Bean("caffeineCacheManager")
  public CacheManager caffeineCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();

    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .initialCapacity(CAFFEINE_INITIAL_CAPACITY) // 初始容量
            .maximumSize(CAFFEINE_MAXIMUM_SIZE) // 最大条目数
            .expireAfterWrite(CAFFEINE_EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES) // 写入后 5 分钟过期
            .expireAfterAccess(CAFFEINE_EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES) // 访问后 3 分钟过期
            .recordStats()); // 启用统计

    // 允许缓存 null 值（用于防止缓存穿透）
    cacheManager.setAllowNullValues(true);

    log.info("Caffeine 本地缓存管理器初始化完成");

    return cacheManager;
  }
}
