package com.lawfirm.infrastructure.monitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定时任务监控器
 *
 * <p>用于追踪定时任务的执行状态、耗时、成功/失败次数
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component
public class ScheduledTaskMonitor {

  /** 任务执行记录 */
  private final Map<String, TaskExecutionRecord> taskRecords = new ConcurrentHashMap<>();

  /**
   * 记录任务开始.
   *
   * @param taskName 任务名称
   * @return 任务执行上下文
   */
  public TaskExecution startTask(final String taskName) {
    TaskExecution execution = new TaskExecution(taskName, LocalDateTime.now());
    log.debug("定时任务开始: {}", taskName);
    return execution;
  }

  /**
   * 记录任务成功完成.
   *
   * @param execution 任务执行上下文
   */
  public void recordSuccess(final TaskExecution execution) {
    long duration = Duration.between(execution.getStartTime(), LocalDateTime.now()).toMillis();

    TaskExecutionRecord record =
        taskRecords.computeIfAbsent(execution.getTaskName(), k -> new TaskExecutionRecord(k));
    record.recordSuccess(duration);

    log.info(
        "定时任务完成: {} | 耗时: {}ms | 总执行: {}次 | 成功率: {}%",
        execution.getTaskName(), duration, record.getTotalCount(), record.getSuccessRate());
  }

  /**
   * 记录任务失败.
   *
   * @param execution 任务执行上下文
   * @param error 错误信息
   */
  public void recordFailure(final TaskExecution execution, final Throwable error) {
    long duration = Duration.between(execution.getStartTime(), LocalDateTime.now()).toMillis();

    TaskExecutionRecord record =
        taskRecords.computeIfAbsent(execution.getTaskName(), k -> new TaskExecutionRecord(k));
    record.recordFailure(duration, error.getMessage());

    log.error(
        "定时任务失败: {} | 耗时: {}ms | 错误: {} | 总执行: {}次 | 成功率: {}%",
        execution.getTaskName(),
        duration,
        error.getMessage(),
        record.getTotalCount(),
        record.getSuccessRate());
  }

  /**
   * 获取任务执行记录.
   *
   * @param taskName 任务名称
   * @return 任务执行记录
   */
  public TaskExecutionRecord getTaskRecord(final String taskName) {
    return taskRecords.get(taskName);
  }

  /**
   * 获取所有任务执行记录.
   *
   * @return 所有任务执行记录
   */
  public Map<String, TaskExecutionRecord> getAllTaskRecords() {
    return new ConcurrentHashMap<>(taskRecords);
  }

  /** 任务执行上下文 */
  @Data
  @AllArgsConstructor
  public static class TaskExecution {
    /** 任务名称 */
    private String taskName;

    /** 开始时间 */
    private LocalDateTime startTime;
  }

  /** 任务执行记录. */
  @Data
  public static class TaskExecutionRecord {
    /** 任务名称. */
    private String taskName;

    /** 总执行次数. */
    private long totalCount;

    /** 成功次数. */
    private long successCount;

    /** 失败次数. */
    private long failureCount;

    /** 总执行时长. */
    private long totalDuration;

    /** 最大执行时长. */
    private long maxDuration;

    /** 最小执行时长 */
    private long minDuration = Long.MAX_VALUE;

    /** 最后执行时间 */
    private LocalDateTime lastExecutionTime;

    /** 最后错误信息 */
    private String lastError;

    /**
     * 构造函数.
     *
     * @param taskName 任务名称
     */
    public TaskExecutionRecord(final String taskName) {
      this.taskName = taskName;
    }

    /**
     * 记录成功执行.
     *
     * @param duration 执行时长
     */
    public synchronized void recordSuccess(final long duration) {
      totalCount++;
      successCount++;
      totalDuration += duration;
      maxDuration = Math.max(maxDuration, duration);
      minDuration = Math.min(minDuration, duration);
      lastExecutionTime = LocalDateTime.now();
    }

    /**
     * 记录失败执行.
     *
     * @param duration 执行时长
     * @param error 错误信息
     */
    public synchronized void recordFailure(final long duration, final String error) {
      totalCount++;
      failureCount++;
      totalDuration += duration;
      maxDuration = Math.max(maxDuration, duration);
      minDuration = Math.min(minDuration, duration);
      lastExecutionTime = LocalDateTime.now();
      lastError = error;
    }

    /**
     * 获取成功率.
     *
     * @return 成功率（百分比）
     */
    public double getSuccessRate() {
      if (totalCount == 0) {
        return 100.0;
      }
      final double percentageFactor = 10000.0;
      final double percentagePrecision = 100.0;
      return Math.round(successCount * percentageFactor / totalCount) / percentagePrecision;
    }

    /**
     * 获取平均执行时长.
     *
     * @return 平均执行时长（毫秒）
     */
    public long getAvgDuration() {
      if (totalCount == 0) {
        return 0;
      }
      return totalDuration / totalCount;
    }
  }
}
