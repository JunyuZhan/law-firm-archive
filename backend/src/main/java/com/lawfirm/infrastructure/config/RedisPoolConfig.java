package com.lawfirm.infrastructure.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;

import java.time.Duration;

/**
 * Redis 连接池优化配置
 * 
 * 优化说明：
 * 1. 使用连接池避免频繁创建连接
 * 2. 配置合理的超时时间
 * 3. 启用 TCP KeepAlive
 * 4. 配置自动重连
 * 
 * @author junyuzhan
 */
@Slf4j
@Configuration
@ConditionalOnClass(LettuceConnectionFactory.class)
public class RedisPoolConfig {

    /**
     * 配置 Lettuce 客户端选项
     */
    @Bean
    public LettuceClientConfiguration lettuceClientConfiguration(RedisProperties redisProperties) {
        // Socket 选项
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .keepAlive(true)  // 启用 TCP KeepAlive
                .build();

        // 客户端选项
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .autoReconnect(true)  // 自动重连
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(10)))
                .build();

        // 连接池配置
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        
        // 最大连接数
        poolConfig.setMaxTotal(20);
        
        // 最大空闲连接
        poolConfig.setMaxIdle(10);
        
        // 最小空闲连接
        poolConfig.setMinIdle(2);
        
        // 获取连接最大等待时间
        poolConfig.setMaxWait(Duration.ofSeconds(5));
        
        // 空闲连接检测周期
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        
        // 空闲连接最小生存时间
        poolConfig.setMinEvictableIdleTime(Duration.ofMinutes(5));
        
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

        log.info("Redis 连接池配置完成: maxTotal={}, maxIdle={}, minIdle={}",
                poolConfig.getMaxTotal(),
                poolConfig.getMaxIdle(),
                poolConfig.getMinIdle());

        return LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .commandTimeout(redisProperties.getTimeout() != null ? 
                        redisProperties.getTimeout() : Duration.ofSeconds(10))
                .build();
    }
}

