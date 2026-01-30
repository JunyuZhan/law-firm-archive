package com.lawfirm.infrastructure.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

/**
 * Redis 连接池优化配置
 *
 * <p>优化说明： 1. 使用连接池避免频繁创建连接 2. 配置合理的超时时间 3. 启用 TCP KeepAlive 4. 配置自动重连
 *
 * @author junyuzhan
 */
@Slf4j
@Configuration
@ConditionalOnClass(LettuceConnectionFactory.class)
public class RedisPoolConfig {

  /** 连接超时时间（秒）. */
  private static final long CONNECT_TIMEOUT_SECONDS = 10L;

  /** 命令超时时间（秒）. */
  private static final long COMMAND_TIMEOUT_SECONDS = 10L;

  /** 最大连接数. */
  private static final int MAX_TOTAL_CONNECTIONS = 20;

  /** 最大空闲连接数. */
  private static final int MAX_IDLE_CONNECTIONS = 10;

  /** 最小空闲连接数. */
  private static final int MIN_IDLE_CONNECTIONS = 2;

  /** 获取连接最大等待时间（秒）. */
  private static final long MAX_WAIT_SECONDS = 5L;

  /** 空闲连接检测周期（秒）. */
  private static final long TIME_BETWEEN_EVICTION_RUNS_SECONDS = 30L;

  /** 空闲连接最小生存时间（分钟）. */
  private static final long MIN_EVICTABLE_IDLE_MINUTES = 5L;

  /**
   * 配置 Lettuce 客户端选项.
   *
   * @param redisProperties Redis属性
   * @return Lettuce客户端配置
   */
  @Bean
  public LettuceClientConfiguration lettuceClientConfiguration(
      final RedisProperties redisProperties) {
    // Socket 选项
    SocketOptions socketOptions =
        SocketOptions.builder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .keepAlive(true) // 启用 TCP KeepAlive
            .build();

    // 客户端选项
    ClientOptions clientOptions =
        ClientOptions.builder()
            .socketOptions(socketOptions)
            .autoReconnect(true) // 自动重连
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS)))
            .build();

    // 连接池配置
    GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();

    // 最大连接数
    poolConfig.setMaxTotal(MAX_TOTAL_CONNECTIONS);

    // 最大空闲连接
    poolConfig.setMaxIdle(MAX_IDLE_CONNECTIONS);

    // 最小空闲连接
    poolConfig.setMinIdle(MIN_IDLE_CONNECTIONS);

    // 获取连接最大等待时间
    poolConfig.setMaxWait(Duration.ofSeconds(MAX_WAIT_SECONDS));

    // 空闲连接检测周期
    poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(TIME_BETWEEN_EVICTION_RUNS_SECONDS));

    // 空闲连接最小生存时间
    poolConfig.setMinEvictableIdleDuration(Duration.ofMinutes(MIN_EVICTABLE_IDLE_MINUTES));

    // 空闲连接测试（借用时检测）
    poolConfig.setTestOnBorrow(true);

    // 空闲连接测试（归还时检测）
    poolConfig.setTestOnReturn(false);

    // 空闲连接测试（空闲时检测）
    poolConfig.setTestWhileIdle(true);

    // 阻塞时公平获取连接
    poolConfig.setFairness(false);

    // LIFO（后进先出），提高连接复用率
    poolConfig.setLifo(true);

    log.info(
        "Redis 连接池配置完成: maxTotal={}, maxIdle={}, minIdle={}",
        poolConfig.getMaxTotal(),
        poolConfig.getMaxIdle(),
        poolConfig.getMinIdle());

    return LettucePoolingClientConfiguration.builder()
        .poolConfig(poolConfig)
        .clientOptions(clientOptions)
        .commandTimeout(
            redisProperties.getTimeout() != null
                ? redisProperties.getTimeout()
                : Duration.ofSeconds(COMMAND_TIMEOUT_SECONDS))
        .build();
  }
}
