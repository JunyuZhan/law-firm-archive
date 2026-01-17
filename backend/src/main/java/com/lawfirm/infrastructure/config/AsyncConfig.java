package com.lawfirm.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 
 * 优化说明：
 * 1. 配置核心线程数、最大线程数、队列容量
 * 2. 使用 CallerRunsPolicy 作为拒绝策略，避免任务丢失
 * 3. 配置线程前缀，便于日志追踪
 * 4. 添加异常处理器，记录异步任务异常
 * 
 * @author junyuzhan
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * CPU 核心数
     */
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * 核心线程数 = CPU核心数 + 1（IO密集型任务推荐）
     */
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;

    /**
     * 最大线程数 = CPU核心数 * 2
     */
    private static final int MAX_POOL_SIZE = CPU_COUNT * 2;

    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 500;

    /**
     * 线程存活时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 默认异步任务执行器
     */
    @Override
    @Bean("asyncExecutor")
    public Executor getAsyncExecutor() {
        log.info("初始化异步任务线程池: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
        
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
        executor.setAwaitTerminationSeconds(60);
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 初始化
        executor.initialize();
        
        return executor;
    }

    /**
     * 邮件/通知专用执行器
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("notify-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 通用任务执行器（报表生成等）
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);  // 报表生成可能耗时较长
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 备份任务专用执行器（单线程，避免并发备份）
     */
    @Bean("backupExecutor")
    public Executor backupExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setKeepAliveSeconds(0);
        executor.setThreadNamePrefix("backup-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(300);  // 备份可能耗时较长
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 异步任务异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * 异步任务异常处理器实现
     */
    @Slf4j
    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("异步任务执行异常: method={}, params={}", 
                    method.getDeclaringClass().getSimpleName() + "." + method.getName(),
                    params, ex);
        }
    }
}

