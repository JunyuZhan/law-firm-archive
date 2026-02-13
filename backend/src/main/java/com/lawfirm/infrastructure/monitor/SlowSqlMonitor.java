package com.lawfirm.infrastructure.monitor;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 慢SQL监控器
 *
 * <p>用于记录和告警慢查询
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component
public class SlowSqlMonitor {

  /** 慢SQL阈值（毫秒），默认1000ms. */
  @Value("${app.monitor.slow-sql-threshold:1000}")
  private long slowSqlThreshold;

  /** 最近的慢SQL记录（保留最近50条）. */
  private final Queue<SlowSqlRecord> recentSlowSqls = new ConcurrentLinkedQueue<>();

  /** 最大记录数. */
  private static final int MAX_RECORDS = 50;

  /** SQL截断长度. */
  private static final int SQL_TRUNCATE_LENGTH = 500;

  /** 参数截断长度. */
  private static final int PARAM_TRUNCATE_LENGTH = 50;

  /** 最大参数显示数量. */
  private static final int MAX_PARAMS_DISPLAY = 10;

  /** 严重慢SQL阈值（毫秒）. */
  private static final long SEVERE_SLOW_SQL_THRESHOLD = 5000L;

  /** 百分比计算因子. */
  private static final double PERCENTAGE_FACTOR = 10000.0;

  /** 百分比精度因子. */
  private static final double PERCENTAGE_PRECISION = 100.0;

  /** 最大调用栈深度. */
  private static final int MAX_STACK_DEPTH = 3;

  /** 统计计数器. */
  private final AtomicLong totalQueries = new AtomicLong(0);

  /** 慢查询计数器. */
  private final AtomicLong slowQueries = new AtomicLong(0);

  /**
   * 记录SQL执行
   *
   * @param sql SQL语句
   * @param duration 执行耗时（毫秒）
   * @param params 参数
   */
  public void recordSql(final String sql, final long duration, final Object[] params) {
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
      log.warn(
          "慢SQL告警 | 耗时: {}ms | SQL: {} | 参数: {}", duration, record.getSql(), record.getParams());

      // 超过5秒的SQL记录错误日志
      if (duration >= SEVERE_SLOW_SQL_THRESHOLD) {
        log.error(
            "严重慢SQL | 耗时: {}ms | SQL: {} | 调用栈: {}",
            duration,
            record.getSql(),
            record.getStackTrace());
      }
    }
  }

  /**
   * 获取慢SQL统计.
   *
   * @return 慢SQL统计信息
   */
  public SlowSqlStats getStats() {
    SlowSqlStats stats = new SlowSqlStats();
    stats.setTotalQueries(totalQueries.get());
    stats.setSlowQueries(slowQueries.get());
    stats.setSlowRate(
        totalQueries.get() > 0
            ? Math.round(slowQueries.get() * PERCENTAGE_FACTOR / totalQueries.get())
                / PERCENTAGE_PRECISION
            : 0);
    stats.setThreshold(slowSqlThreshold);
    return stats;
  }

  /**
   * 获取最近的慢SQL记录.
   *
   * @return 慢SQL记录队列
   */
  public Queue<SlowSqlRecord> getRecentSlowSqls() {
    return new ConcurrentLinkedQueue<>(recentSlowSqls);
  }

  /** 重置统计. */
  public void resetStats() {
    totalQueries.set(0);
    slowQueries.set(0);
    recentSlowSqls.clear();
    log.info("慢SQL统计已重置");
  }

  private void addRecord(final SlowSqlRecord record) {
    recentSlowSqls.offer(record);
    while (recentSlowSqls.size() > MAX_RECORDS) {
      recentSlowSqls.poll();
    }
  }

  private String truncateSql(final String sql) {
    if (sql == null) {
      return null;
    }
    if (sql.length() > SQL_TRUNCATE_LENGTH) {
      return sql.substring(0, SQL_TRUNCATE_LENGTH) + "...";
    }
    return sql;
  }

  private String formatParams(final Object[] params) {
    if (params == null || params.length == 0) {
      return "[]";
    }
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < Math.min(params.length, MAX_PARAMS_DISPLAY); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      Object param = params[i];
      if (param == null) {
        sb.append("null");
      } else {
        String str = param.toString();
        if (str.length() > PARAM_TRUNCATE_LENGTH) {
          str = str.substring(0, PARAM_TRUNCATE_LENGTH) + "...";
        }
        sb.append(str);
      }
    }
    if (params.length > MAX_PARAMS_DISPLAY) {
      sb.append(", ... (").append(params.length - MAX_PARAMS_DISPLAY).append(" more)");
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
        if (count > 0) {
          sb.append(" <- ");
        }
        sb.append(element.getClassName().substring(element.getClassName().lastIndexOf('.') + 1))
            .append(".")
            .append(element.getMethodName())
            .append(":")
            .append(element.getLineNumber());
        if (++count >= MAX_STACK_DEPTH) {
          break;
        }
      }
    }
    return sb.toString();
  }

  /** 慢SQL记录. */
  @Data
  public static class SlowSqlRecord {
    /** SQL语句. */
    private String sql;

    /** 执行时长. */
    private long duration;

    /** 参数. */
    private String params;

    /** 时间戳. */
    private LocalDateTime timestamp;

    /** 调用栈. */
    private String stackTrace;
  }

  /** 慢SQL统计信息. */
  @Data
  public static class SlowSqlStats {
    /** 总查询数. */
    private long totalQueries;

    /** 慢查询数. */
    private long slowQueries;

    /** 慢查询率. */
    private double slowRate;

    /** 阈值. */
    private long threshold;
  }
}
