package com.lawfirm.infrastructure.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 慢SQL监控器
 * 
 * 用于记录和告警慢查询
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component
public class SlowSqlMonitor {

    /**
     * 慢SQL阈值（毫秒），默认1000ms
     */
    @Value("${app.monitor.slow-sql-threshold:1000}")
    private long slowSqlThreshold;

    /**
     * 最近的慢SQL记录（保留最近50条）
     */
    private final Queue<SlowSqlRecord> recentSlowSqls = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECORDS = 50;

    /**
     * 统计计数器
     */
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);

    /**
     * 记录SQL执行
     * 
     * @param sql SQL语句
     * @param duration 执行耗时（毫秒）
     * @param params 参数
     */
    public void recordSql(String sql, long duration, Object[] params) {
        totalQueries.incrementAndGet();
        
        if (duration >= slowSqlThreshold) {
            slowQueries.incrementAndGet();
            
            SlowSqlRecord record = new SlowSqlRecord();
            record.setSql(truncateSql(sql));
            record.setDuration(duration);
            record.setParams(formatParams(params));
            record.setTimestamp(LocalDateTime.now());
            record.setStackTrace(getCallerStackTrace());
            
            addRecord(record);
            
            // 记录警告日志
            log.warn("慢SQL告警 | 耗时: {}ms | SQL: {} | 参数: {}", 
                    duration, record.getSql(), record.getParams());
            
            // 超过5秒的SQL记录错误日志
            if (duration >= 5000) {
                log.error("严重慢SQL | 耗时: {}ms | SQL: {} | 调用栈: {}", 
                        duration, record.getSql(), record.getStackTrace());
            }
        }
    }

    /**
     * 获取慢SQL统计
     */
    public SlowSqlStats getStats() {
        SlowSqlStats stats = new SlowSqlStats();
        stats.setTotalQueries(totalQueries.get());
        stats.setSlowQueries(slowQueries.get());
        stats.setSlowRate(totalQueries.get() > 0 
                ? Math.round(slowQueries.get() * 10000.0 / totalQueries.get()) / 100.0 
                : 0);
        stats.setThreshold(slowSqlThreshold);
        return stats;
    }

    /**
     * 获取最近的慢SQL记录
     */
    public Queue<SlowSqlRecord> getRecentSlowSqls() {
        return new ConcurrentLinkedQueue<>(recentSlowSqls);
    }

    /**
     * 重置统计
     */
    public void resetStats() {
        totalQueries.set(0);
        slowQueries.set(0);
        recentSlowSqls.clear();
        log.info("慢SQL统计已重置");
    }

    private void addRecord(SlowSqlRecord record) {
        recentSlowSqls.offer(record);
        while (recentSlowSqls.size() > MAX_RECORDS) {
            recentSlowSqls.poll();
        }
    }

    private String truncateSql(String sql) {
        if (sql == null) return null;
        if (sql.length() > 500) {
            return sql.substring(0, 500) + "...";
        }
        return sql;
    }

    private String formatParams(Object[] params) {
        if (params == null || params.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(params.length, 10); i++) {
            if (i > 0) sb.append(", ");
            Object param = params[i];
            if (param == null) {
                sb.append("null");
            } else {
                String str = param.toString();
                if (str.length() > 50) {
                    str = str.substring(0, 50) + "...";
                }
                sb.append(str);
            }
        }
        if (params.length > 10) {
            sb.append(", ... (").append(params.length - 10).append(" more)");
        }
        sb.append("]");
        return sb.toString();
    }

    private String getCallerStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.startsWith("com.lawfirm") 
                    && !className.contains("SlowSqlMonitor")
                    && !className.contains("Interceptor")) {
                if (count > 0) sb.append(" <- ");
                sb.append(element.getClassName().substring(element.getClassName().lastIndexOf('.') + 1))
                  .append(".")
                  .append(element.getMethodName())
                  .append(":")
                  .append(element.getLineNumber());
                if (++count >= 3) break;
            }
        }
        return sb.toString();
    }

    @Data
    public static class SlowSqlRecord {
        private String sql;
        private long duration;
        private String params;
        private LocalDateTime timestamp;
        private String stackTrace;
    }

    @Data
    public static class SlowSqlStats {
        private long totalQueries;
        private long slowQueries;
        private double slowRate;
        private long threshold;
    }
}
