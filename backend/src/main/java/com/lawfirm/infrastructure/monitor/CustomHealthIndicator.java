package com.lawfirm.infrastructure.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义健康检查指示器
 * 
 * 增强 /actuator/health 端点，提供更详细的系统健康信息
 * 
 * @author system
 * @since 2026-01-10
 */
@Slf4j
@Component("customHealth")
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean healthy = true;

        // 1. 检查数据库连接
        try {
            long dbStart = System.currentTimeMillis();
            try (Connection conn = dataSource.getConnection()) {
                conn.isValid(5);
            }
            long dbTime = System.currentTimeMillis() - dbStart;
            details.put("database", Map.of(
                "status", "UP",
                "responseTime", dbTime + "ms"
            ));
        } catch (Exception e) {
            healthy = false;
            details.put("database", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
            log.error("数据库健康检查失败", e);
        }

        // 2. 检查Redis连接
        try {
            long redisStart = System.currentTimeMillis();
            redisTemplate.opsForValue().get("health:check");
            long redisTime = System.currentTimeMillis() - redisStart;
            details.put("redis", Map.of(
                "status", "UP",
                "responseTime", redisTime + "ms"
            ));
        } catch (Exception e) {
            healthy = false;
            details.put("redis", Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
            log.error("Redis健康检查失败", e);
        }

        // 3. 检查JVM内存
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / maxMemory * 100;

        details.put("jvm", Map.of(
            "maxMemory", formatBytes(maxMemory),
            "totalMemory", formatBytes(totalMemory),
            "usedMemory", formatBytes(usedMemory),
            "freeMemory", formatBytes(freeMemory),
            "memoryUsage", String.format("%.2f%%", memoryUsage)
        ));

        // 内存使用超过90%告警
        if (memoryUsage > 90) {
            log.warn("JVM内存使用率过高: {}%", String.format("%.2f", memoryUsage));
        }

        // 4. 检查磁盘空间
        java.io.File root = new java.io.File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double diskUsage = (double) usedSpace / totalSpace * 100;

        details.put("disk", Map.of(
            "totalSpace", formatBytes(totalSpace),
            "usedSpace", formatBytes(usedSpace),
            "freeSpace", formatBytes(freeSpace),
            "diskUsage", String.format("%.2f%%", diskUsage)
        ));

        // 磁盘使用超过85%告警
        if (diskUsage > 85) {
            log.warn("磁盘使用率过高: {}%", String.format("%.2f", diskUsage));
        }

        // 5. 系统信息
        details.put("system", Map.of(
            "processors", runtime.availableProcessors(),
            "javaVersion", System.getProperty("java.version"),
            "osName", System.getProperty("os.name"),
            "osArch", System.getProperty("os.arch")
        ));

        if (healthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
