package com.lawfirm.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 性能优化配置
 *
 * <p>包含： 1. HikariCP 连接池优化配置 2. 连接池监控配置
 *
 * @author junyuzhan
 */
@Slf4j
@Configuration
public class PerformanceConfig {

  /** 获取 CPU 核心数. */
  private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

  /** 连接超时时间（毫秒）. */
  private static final long CONNECTION_TIMEOUT_MS = 30000L;

  /** 空闲超时时间（毫秒）. */
  private static final long IDLE_TIMEOUT_MS = 600000L;

  /** 最大生命周期（毫秒）. */
  private static final long MAX_LIFETIME_MS = 1800000L;

  /** 验证超时时间（毫秒）. */
  private static final long VALIDATION_TIMEOUT_MS = 5000L;

  /** 泄漏检测阈值（毫秒）. */
  private static final long LEAK_DETECTION_THRESHOLD_MS = 120000L;

  /** 最小空闲连接数. */
  private static final int MIN_IDLE_CONNECTIONS = 5;

  /** 最小连接池大小. */
  private static final int MIN_POOL_SIZE = 10;

  /**
   * 优化后的 HikariCP 数据源.
   *
   * <p>连接池大小计算公式（PostgreSQL 推荐）： connections = ((core_count * 2) + effective_spindle_count) 对于
   * SSD，effective_spindle_count = 1
   *
   * <p>参考：https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
   *
   * @param properties 数据源属性
   * @return 数据源
   */
  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource.hikari")
  public DataSource dataSource(final DataSourceProperties properties) {
    HikariDataSource dataSource =
        properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();

    // 连接池名称
    dataSource.setPoolName("LawFirmHikariPool");

    // 计算最优连接池大小
    int poolSize = (CPU_COUNT * 2) + 1;

    // 最小空闲连接数
    dataSource.setMinimumIdle(Math.min(MIN_IDLE_CONNECTIONS, poolSize));

    // 最大连接数
    dataSource.setMaximumPoolSize(Math.max(poolSize, MIN_POOL_SIZE));

    // 连接超时（30秒）
    dataSource.setConnectionTimeout(CONNECTION_TIMEOUT_MS);

    // 空闲超时（10分钟）
    dataSource.setIdleTimeout(IDLE_TIMEOUT_MS);

    // 连接最大生命周期（30分钟，要小于数据库的 wait_timeout）
    dataSource.setMaxLifetime(MAX_LIFETIME_MS);

    // 连接验证超时（5秒）
    dataSource.setValidationTimeout(VALIDATION_TIMEOUT_MS);

    // 连接泄漏检测阈值（2分钟，0 表示禁用）
    dataSource.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD_MS);

    // 连接测试查询（PostgreSQL 优化）
    dataSource.setConnectionTestQuery("SELECT 1");

    // 自动提交
    dataSource.setAutoCommit(true);

    // 只读模式（默认 false）
    dataSource.setReadOnly(false);

    // 事务隔离级别（使用数据库默认）
    // dataSource.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

    log.info(
        "HikariCP 连接池配置完成: poolName={}, minIdle={}, maxPoolSize={}",
        dataSource.getPoolName(),
        dataSource.getMinimumIdle(),
        dataSource.getMaximumPoolSize());

    return dataSource;
  }
}
