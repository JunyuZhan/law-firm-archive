package com.archivesystem.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.archivesystem.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 外部依赖健康检查配置.
 * @author junyuzhan
 */
@Configuration
@RequiredArgsConstructor
public class ExternalDependencyHealthConfig {

    private final MinioService minioService;
    private final ElasticsearchClient elasticsearchClient;

    @Bean
    public HealthIndicator minioHealthIndicator() {
        return () -> minioService.isBucketAccessible()
                ? Health.up().withDetail("bucket", minioService.getBucketName()).build()
                : Health.down().withDetail("bucket", minioService.getBucketName()).build();
    }

    @Bean
    public HealthIndicator elasticsearchHealthIndicator() {
        return () -> {
            try {
                boolean reachable = Boolean.TRUE.equals(elasticsearchClient.ping().value());
                if (reachable) {
                    return Health.up().build();
                }
                return Health.down().withDetail("message", "ping returned false").build();
            } catch (Exception e) {
                return Health.down(e).build();
            }
        };
    }
}
