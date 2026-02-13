package com.lawfirm.infrastructure.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务线程池配置
 *
 * <p>优化说明： 1. 配置核心线程数、最大线程数、队列容量 2. 使用 CallerRunsPolicy 作为拒绝策略，避免任务丢失 3. 配置线程前缀，便于日志追踪 4.
 * 添加异常处理器，记录异步任务异常
 *
 * @author junyuzhan
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

  /** CPU 核心数. */
  private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

  /** 核心线程数 = CPU核心数 + 1（IO密集型任务推荐）. */
  private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

  /** 最大线程数 = CPU核心数 * 2. */
  private static final int MAX_POOL_SIZE = CPU_COUNT * 2;

  /** 队列容量. */
  private static final int QUEUE_CAPACITY = 500;

  /** 线程存活时间（秒）. */
  private static final int KEEP_ALIVE_SECONDS = 60;

  /** 通知执行器核心线程数. */
  private static final int NOTIFICATION_CORE_POOL_SIZE = 2;

  /** 通知执行器最大线程数. */
  private static final int NOTIFICATION_MAX_POOL_SIZE = 5;

  /** 通知执行器队列容量. */
  private static final int NOTIFICATION_QUEUE_CAPACITY = 200;

  /** 通知执行器等待时间（秒）. */
  private static final int NOTIFICATION_AWAIT_TERMINATION_SECONDS = 30;

  /** 任务执行器队列容量. */
  private static final int TASK_QUEUE_CAPACITY = 100;

  /** 任务执行器等待时间（秒）. */
  private static final int TASK_AWAIT_TERMINATION_SECONDS = 120;

  /** 备份执行器核心线程数. */
  private static final int BACKUP_CORE_POOL_SIZE = 1;

  /** 备份执行器最大线程数. */
  private static final int BACKUP_MAX_POOL_SIZE = 2;

  /** 备份执行器队列容量. */
  private static final int BACKUP_QUEUE_CAPACITY = 10;

  /** 备份执行器等待时间（秒）. */
  private static final int BACKUP_AWAIT_TERMINATION_SECONDS = 300;

  /**
   * 默认异步任务执行器.
   *
   * @return 异步执行器
   */
  @Override
  @Bean("asyncExecutor")
  public Executor getAsyncExecutor() {
    log.info(
        "初始化异步任务线程池: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
        CORE_POOL_SIZE,
        MAX_POOL_SIZE,
        QUEUE_CAPACITY);

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // 核心线程数
    executor.setCorePoolSize(CORE_POOL_SIZE);
    // 最大线程数
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    // 队列容量
    executor.setQueueCapacity(QUEUE_CAPACITY);
    // 线程存活时间
    executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
    // 线程名前缀
    executor.setThreadNamePrefix("async-");
    // 等待所有任务完成再关闭线程池
    executor.setWaitForTasksToCompleteOnShutdown(true);
    // 等待时间（秒）
    executor.setAwaitTerminationSeconds(KEEP_ALIVE_SECONDS);
    // 拒绝策略：由调用线程执行
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    // 初始化
    executor.initialize();

    return executor;
  }

  /**
   * 邮件/通知专用执行器.
   *
   * @return 通知执行器
   */
  @Bean("notificationExecutor")
  public Executor notificationExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(NOTIFICATION_CORE_POOL_SIZE);
    executor.setMaxPoolSize(NOTIFICATION_MAX_POOL_SIZE);
    executor.setQueueCapacity(NOTIFICATION_QUEUE_CAPACITY);
    executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
    executor.setThreadNamePrefix("notify-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(NOTIFICATION_AWAIT_TERMINATION_SECONDS);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 通用任务执行器（报表生成等）.
   *
   * @return 任务执行器
   */
  @Bean("taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(CORE_POOL_SIZE);
    executor.setMaxPoolSize(MAX_POOL_SIZE);
    executor.setQueueCapacity(TASK_QUEUE_CAPACITY);
    executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
    executor.setThreadNamePrefix("task-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(TASK_AWAIT_TERMINATION_SECONDS); // 报表生成可能耗时较长
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 备份任务专用执行器（单线程，避免并发备份）.
   *
   * @return 备份执行器
   */
  @Bean("backupExecutor")
  public Executor backupExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(BACKUP_CORE_POOL_SIZE);
    executor.setMaxPoolSize(BACKUP_MAX_POOL_SIZE);
    executor.setQueueCapacity(BACKUP_QUEUE_CAPACITY);
    executor.setKeepAliveSeconds(0);
    executor.setThreadNamePrefix("backup-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(BACKUP_AWAIT_TERMINATION_SECONDS); // 备份可能耗时较长
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
    executor.initialize();
    return executor;
  }

  /**
   * 异步任务异常处理器.
   *
   * @return 异常处理器
   */
  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncExceptionHandler();
  }

  /** 异步任务异常处理器实现. */
  @Slf4j
  private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
    /**
     * 处理未捕获的异常.
     *
     * @param ex 异常
     * @param method 方法
     * @param params 参数
     */
    @Override
    public void handleUncaughtException(
        final Throwable ex, final Method method, final Object... params) {
      log.error(
          "异步任务执行异常: method={}, params={}",
          method.getDeclaringClass().getSimpleName() + "." + method.getName(),
          params,
          ex);
    }
  }
}
