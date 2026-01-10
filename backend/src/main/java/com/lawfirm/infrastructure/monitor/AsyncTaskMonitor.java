package com.lawfirm.infrastructure.monitor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 异步任务监控器
 * 
 * 用于追踪异步任务的执行状态、队列深度、执行耗时
 * 
 * @author system
 * @since 2026-01-10
 */
@Slf4j
@Component
public class AsyncTaskMonitor {

    /**
     * 任务统计
     */
    private final Map<String, TaskStats> taskStatsMap = new ConcurrentHashMap<>();

    /**
     * 最近执行的任务（保留最近100条）
     */
    private final Queue<TaskRecord> recentTasks = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_TASKS = 100;

    /**
     * 全局计数器
     */
    private final AtomicLong totalSubmitted = new AtomicLong(0);
    private final AtomicLong totalCompleted = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);

    /**
     * 记录任务提交
     */
    public String submitTask(String taskType, String taskId) {
        totalSubmitted.incrementAndGet();
        
        TaskStats stats = taskStatsMap.computeIfAbsent(taskType, k -> new TaskStats(k));
        stats.incrementSubmitted();
        
        log.debug("异步任务提交: type={}, id={}, 队列深度={}", 
                taskType, taskId, stats.getPendingCount());
        
        return taskId;
    }

    /**
     * 记录任务开始执行
     */
    public long startTask(String taskType, String taskId) {
        TaskStats stats = taskStatsMap.get(taskType);
        if (stats != null) {
            stats.incrementRunning();
        }
        
        log.debug("异步任务开始: type={}, id={}", taskType, taskId);
        return System.currentTimeMillis();
    }

    /**
     * 记录任务完成
     */
    public void completeTask(String taskType, String taskId, long startTime) {
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
     * 记录任务失败
     */
    public void failTask(String taskType, String taskId, long startTime, Throwable error) {
        long duration = System.currentTimeMillis() - startTime;
        totalFailed.incrementAndGet();
        
        TaskStats stats = taskStatsMap.get(taskType);
        if (stats != null) {
            stats.recordFailure(duration);
        }
        
        addRecentTask(new TaskRecord(taskType, taskId, "FAILED", duration, error.getMessage()));
        
        log.error("异步任务失败: type={}, id={}, 耗时={}ms, 错误={}", 
                taskType, taskId, duration, error.getMessage());
    }

    /**
     * 获取监控摘要
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

    private void addRecentTask(TaskRecord record) {
        recentTasks.offer(record);
        while (recentTasks.size() > MAX_RECENT_TASKS) {
            recentTasks.poll();
        }
    }

    @Data
    public static class TaskStats {
        private String taskType;
        private AtomicLong submitted = new AtomicLong(0);
        private AtomicLong completed = new AtomicLong(0);
        private AtomicLong failed = new AtomicLong(0);
        private AtomicLong running = new AtomicLong(0);
        private AtomicLong totalDuration = new AtomicLong(0);

        public TaskStats(String taskType) {
            this.taskType = taskType;
        }

        public void incrementSubmitted() {
            submitted.incrementAndGet();
        }

        public void incrementRunning() {
            running.incrementAndGet();
        }

        public void recordCompletion(long duration) {
            completed.incrementAndGet();
            running.decrementAndGet();
            totalDuration.addAndGet(duration);
        }

        public void recordFailure(long duration) {
            failed.incrementAndGet();
            running.decrementAndGet();
            totalDuration.addAndGet(duration);
        }

        public long getPendingCount() {
            return submitted.get() - completed.get() - failed.get();
        }

        public long getAvgDuration() {
            long total = completed.get() + failed.get();
            return total > 0 ? totalDuration.get() / total : 0;
        }
    }

    @Data
    public static class TaskRecord {
        private String taskType;
        private String taskId;
        private String status;
        private long duration;
        private String error;
        private LocalDateTime timestamp = LocalDateTime.now();

        public TaskRecord(String taskType, String taskId, String status, long duration, String error) {
            this.taskType = taskType;
            this.taskId = taskId;
            this.status = status;
            this.duration = duration;
            this.error = error;
        }
    }

    @Data
    public static class MonitorSummary {
        private long totalSubmitted;
        private long totalCompleted;
        private long totalFailed;
        private long totalPending;
        private Map<String, TaskStats> taskStats;
    }
}
