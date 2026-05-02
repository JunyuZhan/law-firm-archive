package com.archivesystem.integration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.archivesystem.elasticsearch.ArchiveSearchRepository;
import com.archivesystem.service.ArchiveIndexService;
import com.archivesystem.service.MinioService;
import io.minio.MinioClient;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 集成测试配置类.
 * 提供外部依赖的Mock实现.
 * @author junyuzhan
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        return template;
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(template.opsForValue()).thenReturn(valueOps);
        // RateLimitFilter使用Lua脚本execute()，需要mock返回值
        when(template.execute(any(DefaultRedisScript.class), any(List.class), any(String.class))).thenReturn(1L);
        return template;
    }

    @Bean
    @Primary
    public ConnectionFactory rabbitConnectionFactory() {
        return mock(ConnectionFactory.class);
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public MinioClient minioClient() {
        return mock(MinioClient.class);
    }

    @Bean
    @Primary
    public MinioService minioService() {
        return mock(MinioService.class);
    }

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        return mock(ElasticsearchClient.class);
    }

    @Bean
    @Primary
    public ElasticsearchTemplate elasticsearchTemplate() {
        return mock(ElasticsearchTemplate.class);
    }

    @Bean
    @Primary
    public ArchiveSearchRepository archiveSearchRepository() {
        return mock(ArchiveSearchRepository.class);
    }

    @Bean
    @Primary
    public ArchiveIndexService archiveIndexService() {
        return mock(ArchiveIndexService.class);
    }
}
