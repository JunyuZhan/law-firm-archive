package com.lawfirm.infrastructure.monitor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务监控器
 * 
 * 用于追踪定时任务的执行状态、耗时、成功/失败次数
 * 
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component
public class ScheduledTaskMonitor {

    /**
     * 任务执行记录
     */
    private final Map<String, TaskExecutionRecord> taskRecords = new ConcurrentHashMap<>();

    /**
     * 记录任务开始
     */
    public TaskExecution startTask(String taskName) {
        TaskExecution execution = new TaskExecution(taskName, LocalDateTime.now());
        log.debug("定时任务开始: {}", taskName);
        return execution;
    }

    /**
     * 记录任务成功完成
     */
    public void recordSuccess(TaskExecution execution) {
        long duration = Duration.between(execution.getStartTime(), LocalDateTime.now()).toMillis();
        
        TaskExecutionRecord record = taskRecords.computeIfAbsent(
            execution.getTaskName(), 
            k -> new TaskExecutionRecord(k)
        );
        record.recordSuccess(duration);
        
        log.info("定时任务完成: {} | 耗时: {}ms | 总执行: {}次 | 成功率: {}%",
                execution.getTaskName(), duration, 
                record.getTotalCount(), record.getSuccessRate());
    }

    /**
     * 记录任务失败
     */
    public void recordFailure(TaskExecution execution, Throwable error) {
        long duration = Duration.between(execution.getStartTime(), LocalDateTime.now()).toMillis();
        
        TaskExecutionRecord record = taskRecords.computeIfAbsent(
            execution.getTaskName(), 
            k -> new TaskExecutionRecord(k)
        );
        record.recordFailure(duration, error.getMessage());
        
        log.error("定时任务失败: {} | 耗时: {}ms | 错误: {} | 总执行: {}次 | 成功率: {}%",
                execution.getTaskName(), duration, error.getMessage(),
                record.getTotalCount(), record.getSuccessRate());
    }

    /**
     * 获取任务执行记录
     */
    public TaskExecutionRecord getTaskRecord(String taskName) {
        return taskRecords.get(taskName);
    }

    /**
     * 获取所有任务执行记录
     */
    public Map<String, TaskExecutionRecord> getAllTaskRecords() {
        return new ConcurrentHashMap<>(taskRecords);
    }

    /**
     * 任务执行上下文
     */
    @Data
    @AllArgsConstructor
    public static class TaskExecution {
        private String taskName;
        private LocalDateTime startTime;
    }

    /**
     * 任务执行记录
     */
    @Data
    public static class TaskExecutionRecord {
        private String taskName;
        private long totalCount;
        private long successCount;
        private long failureCount;
        private long totalDuration;
        private long maxDuration;
        private long minDuration = Long.MAX_VALUE;
        private LocalDateTime lastExecutionTime;
        private String lastError;

        public TaskExecutionRecord(String taskName) {
            this.taskName = taskName;
        }

        public synchronized void recordSuccess(long duration) {
            totalCount++;
            successCount++;
            totalDuration += duration;
            maxDuration = Math.max(maxDuration, duration);
            minDuration = Math.min(minDuration, duration);
            lastExecutionTime = LocalDateTime.now();
        }

        public synchronized void recordFailure(long duration, String error) {
            totalCount++;
            failureCount++;
            totalDuration += duration;
            maxDuration = Math.max(maxDuration, duration);
            minDuration = Math.min(minDuration, duration);
            lastExecutionTime = LocalDateTime.now();
            lastError = error;
        }

        public double getSuccessRate() {
            if (totalCount == 0) return 100.0;
            return Math.round(successCount * 10000.0 / totalCount) / 100.0;
        }

        public long getAvgDuration() {
            if (totalCount == 0) return 0;
            return totalDuration / totalCount;
        }
    }
}
