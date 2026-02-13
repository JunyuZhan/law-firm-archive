package com.lawfirm.infrastructure.monitor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 异步任务监控器
 *
 * <p>用于追踪异步任务的执行状态、队列深度、执行耗时
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component
public class AsyncTaskMonitor {

  /** 任务统计. */
  private final Map<String, TaskStats> taskStatsMap = new ConcurrentHashMap<>();

  /** 最近执行的任务（保留最近100条）. */
  private final Queue<TaskRecord> recentTasks = new ConcurrentLinkedQueue<>();

  /** 最大保留任务数. */
  private static final int MAX_RECENT_TASKS = 100;

  /** 全局计数器 - 总提交数. */
  private final AtomicLong totalSubmitted = new AtomicLong(0);

  /** 全局计数器 - 总完成数. */
  private final AtomicLong totalCompleted = new AtomicLong(0);

  /** 全局计数器 - 总失败数. */
  private final AtomicLong totalFailed = new AtomicLong(0);

  /**
   * 记录任务提交.
   *
   * @param taskType 任务类型
   * @param taskId 任务ID
   * @return 任务ID
   */
  public String submitTask(final String taskType, final String taskId) {
    totalSubmitted.incrementAndGet();

    TaskStats stats = taskStatsMap.computeIfAbsent(taskType, k -> new TaskStats(k));
    stats.incrementSubmitted();

    log.debug("异步任务提交: type={}, id={}, 队列深度={}", taskType, taskId, stats.getPendingCount());

    return taskId;
  }

  /**
   * 记录任务开始执行.
   *
   * @param taskType 任务类型
   * @param taskId 任务ID
   * @return 开始时间戳
   */
  public long startTask(final String taskType, final String taskId) {
    TaskStats stats = taskStatsMap.get(taskType);
    if (stats != null) {
      stats.incrementRunning();
    }

    log.debug("异步任务开始: type={}, id={}", taskType, taskId);
    return System.currentTimeMillis();
  }

  /**
   * 记录任务完成.
   *
   * @param taskType 任务类型
   * @param taskId 任务ID
   * @param startTime 开始时间戳
   */
  public void completeTask(final String taskType, final String taskId, final long startTime) {
    long duration = System.currentTimeMillis() - startTime;
    totalCompleted.incrementAndGet();

    TaskStats stats = taskStatsMap.get(taskType);
    if (stats != null) {
      stats.recordCompletion(duration);
    }

    addRecentTask(new TaskRecord(taskType, taskId, "SUCCESS", duration, null));

    log.debug("异步任务完成: type={}, id={}, 耗时={}ms", taskType, taskId, duration);
  }

  /**
   * 记录任务失败.
   *
   * @param taskType 任务类型
   * @param taskId 任务ID
   * @param startTime 开始时间戳
   * @param error 错误信息
   */
  public void failTask(
      final String taskType, final String taskId, final long startTime, final Throwable error) {
    long duration = System.currentTimeMillis() - startTime;
    totalFailed.incrementAndGet();

    TaskStats stats = taskStatsMap.get(taskType);
    if (stats != null) {
      stats.recordFailure(duration);
    }

    addRecentTask(new TaskRecord(taskType, taskId, "FAILED", duration, error.getMessage()));

    log.error(
        "异步任务失败: type={}, id={}, 耗时={}ms, 错误={}", taskType, taskId, duration, error.getMessage());
  }

  /**
   * 获取监控摘要.
   *
   * @return 监控摘要
   */
  public MonitorSummary getSummary() {
    MonitorSummary summary = new MonitorSummary();
    summary.setTotalSubmitted(totalSubmitted.get());
    summary.setTotalCompleted(totalCompleted.get());
    summary.setTotalFailed(totalFailed.get());
    summary.setTotalPending(totalSubmitted.get() - totalCompleted.get() - totalFailed.get());
    summary.setTaskStats(new ConcurrentHashMap<>(taskStatsMap));
    return summary;
  }

  /**
   * 添加最近任务记录.
   *
   * @param record 任务记录
   */
  private void addRecentTask(final TaskRecord record) {
    recentTasks.offer(record);
    while (recentTasks.size() > MAX_RECENT_TASKS) {
      recentTasks.poll();
    }
  }

  /** 任务统计信息. */
  @Data
  public static class TaskStats {
    /** 任务类型. */
    private String taskType;

    /** 已提交数量. */
    private AtomicLong submitted = new AtomicLong(0);

    /** 已完成数量. */
    private AtomicLong completed = new AtomicLong(0);

    /** 失败数量. */
    private AtomicLong failed = new AtomicLong(0);

    /** 运行中数量. */
    private AtomicLong running = new AtomicLong(0);

    /** 总执行时长. */
    private AtomicLong totalDuration = new AtomicLong(0);

    /**
     * 构造函数.
     *
     * @param taskType 任务类型
     */
    public TaskStats(final String taskType) {
      this.taskType = taskType;
    }

    /** 增加提交计数. */
    public void incrementSubmitted() {
      submitted.incrementAndGet();
    }

    /** 增加运行计数. */
    public void incrementRunning() {
      running.incrementAndGet();
    }

    /**
     * 记录完成.
     *
     * @param duration 执行时长
     */
    public void recordCompletion(final long duration) {
      completed.incrementAndGet();
      running.decrementAndGet();
      totalDuration.addAndGet(duration);
    }

    /**
     * 记录失败.
     *
     * @param duration 执行时长
     */
    public void recordFailure(final long duration) {
      failed.incrementAndGet();
      running.decrementAndGet();
      totalDuration.addAndGet(duration);
    }

    /**
     * 获取待处理数量.
     *
     * @return 待处理数量
     */
    public long getPendingCount() {
      return submitted.get() - completed.get() - failed.get();
    }

    /**
     * 获取平均执行时长.
     *
     * @return 平均执行时长（毫秒）
     */
    public long getAvgDuration() {
      long total = completed.get() + failed.get();
      return total > 0 ? totalDuration.get() / total : 0;
    }
  }

  /** 任务执行记录. */
  @Data
  public static class TaskRecord {
    /** 任务类型. */
    private String taskType;

    /** 任务ID. */
    private String taskId;

    /** 执行状态. */
    private String status;

    /** 执行时长. */
    private long duration;

    /** 错误信息. */
    private String error;

    /** 时间戳. */
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 构造函数.
     *
     * @param taskType 任务类型
     * @param taskId 任务ID
     * @param status 状态
     * @param duration 执行时长
     * @param error 错误信息
     */
    public TaskRecord(
        final String taskType,
        final String taskId,
        final String status,
        final long duration,
        final String error) {
      this.taskType = taskType;
      this.taskId = taskId;
      this.status = status;
      this.duration = duration;
      this.error = error;
    }
  }

  /** 监控摘要信息. */
  @Data
  public static class MonitorSummary {
    /** 总提交数. */
    private long totalSubmitted;

    /** 总完成数. */
    private long totalCompleted;

    /** 总失败数. */
    private long totalFailed;

    /** 待处理数. */
    private long totalPending;

    /** 任务统计. */
    private Map<String, TaskStats> taskStats;
  }
}
