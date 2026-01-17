package com.lawfirm.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 性能优化配置
 * 
 * 包含：
 * 1. HikariCP 连接池优化配置
 * 2. 连接池监控配置
 * 
 * @author junyuzhan
 */
@Slf4j
@Configuration
public class PerformanceConfig {

    /**
     * 获取 CPU 核心数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 优化后的 HikariCP 数据源
     * 
     * 连接池大小计算公式（PostgreSQL 推荐）：
     * connections = ((core_count * 2) + effective_spindle_count)
     * 对于 SSD，effective_spindle_count = 1
     * 
     * 参考：https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource dataSource = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        
        // 连接池名称
        dataSource.setPoolName("LawFirmHikariPool");
        
        // 计算最优连接池大小
        int poolSize = (CPU_COUNT * 2) + 1;
        
        // 最小空闲连接数
        dataSource.setMinimumIdle(Math.min(5, poolSize));
        
        // 最大连接数
        dataSource.setMaximumPoolSize(Math.max(poolSize, 10));
        
        // 连接超时（30秒）
        dataSource.setConnectionTimeout(30000);
        
        // 空闲超时（10分钟）
        dataSource.setIdleTimeout(600000);
        
        // 连接最大生命周期（30分钟，要小于数据库的 wait_timeout）
        dataSource.setMaxLifetime(1800000);
        
        // 连接验证超时（5秒）
        dataSource.setValidationTimeout(5000);
        
        // 连接泄漏检测阈值（2分钟，0 表示禁用）
        dataSource.setLeakDetectionThreshold(120000);
        
        // 连接测试查询（PostgreSQL 优化）
        dataSource.setConnectionTestQuery("SELECT 1");
        
        // 自动提交
        dataSource.setAutoCommit(true);
        
        // 只读模式（默认 false）
        dataSource.setReadOnly(false);
        
        // 事务隔离级别（使用数据库默认）
        // dataSource.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        
        log.info("HikariCP 连接池配置完成: poolName={}, minIdle={}, maxPoolSize={}",
                dataSource.getPoolName(),
                dataSource.getMinimumIdle(),
                dataSource.getMaximumPoolSize());
        
        return dataSource;
    }
}

