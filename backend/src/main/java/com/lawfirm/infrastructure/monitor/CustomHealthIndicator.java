package com.lawfirm.infrastructure.monitor;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查指示器
 *
 * <p>增强 /actuator/health 端点，提供更详细的系统健康信息
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component("customHealth")
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

  /** 内存使用率告警阈值（百分比）. */
  private static final double MEMORY_USAGE_THRESHOLD = 90.0;

  /** 磁盘使用率告警阈值（百分比）. */
  private static final double DISK_USAGE_THRESHOLD = 85.0;

  /** 字节转换单位. */
  private static final long BYTES_PER_KB = 1024L;

  /** 字节转MB单位. */
  private static final long BYTES_PER_MB = 1024L * 1024L;

  /** 字节转GB单位. */
  private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;

  /** 数据源. */
  private final DataSource dataSource;

  /** Redis模板. */
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 健康检查.
   *
   * @return 健康状态
   */
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
      details.put("database", Map.of("status", "UP", "responseTime", dbTime + "ms"));
    } catch (Exception e) {
      healthy = false;
      details.put("database", Map.of("status", "DOWN", "error", e.getMessage()));
      log.error("数据库健康检查失败", e);
    }

    // 2. 检查Redis连接
    try {
      long redisStart = System.currentTimeMillis();
      redisTemplate.opsForValue().get("health:check");
      long redisTime = System.currentTimeMillis() - redisStart;
      details.put("redis", Map.of("status", "UP", "responseTime", redisTime + "ms"));
    } catch (Exception e) {
      healthy = false;
      details.put("redis", Map.of("status", "DOWN", "error", e.getMessage()));
      log.error("Redis健康检查失败", e);
    }

    // 3. 检查JVM内存
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;
    double memoryUsage = (double) usedMemory / maxMemory * 100;

    details.put(
        "jvm",
        Map.of(
            "maxMemory", formatBytes(maxMemory),
            "totalMemory", formatBytes(totalMemory),
            "usedMemory", formatBytes(usedMemory),
            "freeMemory", formatBytes(freeMemory),
            "memoryUsage", String.format("%.2f%%", memoryUsage)));

    // 内存使用超过90%告警
    if (memoryUsage > MEMORY_USAGE_THRESHOLD) {
      log.warn("JVM内存使用率过高: {}%", String.format("%.2f", memoryUsage));
    }

    // 4. 检查磁盘空间
    java.io.File root = new java.io.File("/");
    long totalSpace = root.getTotalSpace();
    long freeSpace = root.getFreeSpace();
    long usedSpace = totalSpace - freeSpace;
    double diskUsage = (double) usedSpace / totalSpace * 100;

    details.put(
        "disk",
        Map.of(
            "totalSpace", formatBytes(totalSpace),
            "usedSpace", formatBytes(usedSpace),
            "freeSpace", formatBytes(freeSpace),
            "diskUsage", String.format("%.2f%%", diskUsage)));

    // 磁盘使用超过85%告警
    if (diskUsage > DISK_USAGE_THRESHOLD) {
      log.warn("磁盘使用率过高: {}%", String.format("%.2f", diskUsage));
    }

    // 5. 系统信息
    details.put(
        "system",
        Map.of(
            "processors", runtime.availableProcessors(),
            "javaVersion", System.getProperty("java.version"),
            "osName", System.getProperty("os.name"),
            "osArch", System.getProperty("os.arch")));

    if (healthy) {
      return Health.up().withDetails(details).build();
    } else {
      return Health.down().withDetails(details).build();
    }
  }

  /**
   * 格式化字节数.
   *
   * @param bytes 字节数
   * @return 格式化后的字符串
   */
  private String formatBytes(final long bytes) {
    if (bytes < BYTES_PER_KB) {
      return bytes + " B";
    }
    if (bytes < BYTES_PER_MB) {
      return String.format("%.2f KB", bytes / (double) BYTES_PER_KB);
    }
    if (bytes < BYTES_PER_GB) {
      return String.format("%.2f MB", bytes / (double) BYTES_PER_MB);
    }
    return String.format("%.2f GB", bytes / (double) BYTES_PER_GB);
  }
}
