package com.lawfirm.infrastructure.cache;

import com.lawfirm.common.annotation.CacheWarmUp;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 缓存预热执行器
 *
 * <p>系统启动时自动扫描并执行带有 @CacheWarmUp 注解的方法
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class CacheWarmUpRunner implements ApplicationRunner {

  /** 应用上下文. */
  private final ApplicationContext applicationContext;

  /** 激活的配置文件. */
  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  /** 是否启用缓存预热. */
  @Value("${cache.warmup.enabled:true}")
  private boolean cacheWarmUpEnabled;

  /**
   * 应用启动时执行缓存预热.
   *
   * @param args 应用参数
   * @throws Exception 执行异常
   */
  @Override
  public void run(final ApplicationArguments args) {
    // 检查是否启用预热
    if (!cacheWarmUpEnabled) {
      log.info("缓存预热已禁用 (cache.warmup.enabled=false)");
      return;
    }

    log.info("========== 开始缓存预热 ==========");
    long startTime = System.currentTimeMillis();

    List<WarmUpTask> tasks = collectWarmUpTasks();

    if (tasks.isEmpty()) {
      log.info("未发现需要预热的缓存");
      return;
    }

    // 按order排序
    tasks.sort(Comparator.comparingInt(WarmUpTask::getOrder));

    int successCount = 0;
    int failCount = 0;
    boolean isProduction =
        "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);

    for (WarmUpTask task : tasks) {
      try {
        if (task.isAsync()) {
          executeAsync(task);
        } else {
          executeSync(task);
        }
        successCount++;
      } catch (Exception e) {
        failCount++;
        log.error("缓存预热失败: {} - {}", task.getDescription(), e.getMessage());

        if (!task.isContinueOnError()) {
          if (isProduction) {
            // 生产环境：记录严重警告，但不阻断启动（保证服务可用性）
            log.error("========================================");
            log.error("⚠️ 生产环境检测到严重的缓存预热失败!");
            log.error("⚠️ 失败任务: {}", task.getDescription());
            log.error("⚠️ 为保证服务可用性，系统将继续启动");
            log.error("⚠️ 请立即检查并修复此问题！");
            log.error("========================================");
            // 不抛出异常，允许系统继续启动
          } else {
            // 开发/测试环境：阻断启动，便于发现问题
            throw new RuntimeException("缓存预热失败，系统启动中断（开发环境）", e);
          }
        }
      }
    }

    long duration = System.currentTimeMillis() - startTime;

    if (failCount > 0) {
      log.warn(
          "========== 缓存预热完成（有失败）| 成功: {} | 失败: {} | 耗时: {}ms ==========",
          successCount,
          failCount,
          duration);
    } else {
      log.info(
          "========== 缓存预热完成 | 成功: {} | 失败: {} | 耗时: {}ms ==========",
          successCount,
          failCount,
          duration);
    }
  }

  /**
   * 收集所有预热任务.
   *
   * @return 预热任务列表
   */
  private List<WarmUpTask> collectWarmUpTasks() {
    List<WarmUpTask> tasks = new ArrayList<>();

    Map<String, Object> beans = applicationContext.getBeansOfType(Object.class);

    for (Map.Entry<String, Object> entry : beans.entrySet()) {
      Object bean = entry.getValue();
      Class<?> clazz = bean.getClass();

      // 处理代理类
      if (clazz.getName().contains("$$")) {
        clazz = clazz.getSuperclass();
      }

      for (Method method : clazz.getDeclaredMethods()) {
        CacheWarmUp annotation = method.getAnnotation(CacheWarmUp.class);
        if (annotation != null) {
          WarmUpTask task = new WarmUpTask();
          task.setBean(bean);
          task.setMethod(method);
          task.setKeyPrefix(annotation.keyPrefix());
          task.setOrder(annotation.order());
          task.setDescription(
              annotation.description().isEmpty()
                  ? clazz.getSimpleName() + "." + method.getName()
                  : annotation.description());
          task.setAsync(annotation.async());
          task.setContinueOnError(annotation.continueOnError());
          tasks.add(task);

          log.debug("发现预热任务: {} (order={})", task.getDescription(), task.getOrder());
        }
      }
    }

    return tasks;
  }

  /**
   * 同步执行预热任务.
   *
   * @param task 预热任务
   * @throws Exception 执行异常
   */
  private void executeSync(final WarmUpTask task) throws Exception {
    log.info("预热缓存: {} ...", task.getDescription());
    long start = System.currentTimeMillis();

    task.getMethod().setAccessible(true);
    Object result = task.getMethod().invoke(task.getBean());

    long duration = System.currentTimeMillis() - start;

    int count = 0;
    if (result instanceof java.util.Collection) {
      count = ((java.util.Collection<?>) result).size();
    } else if (result instanceof Map) {
      count = ((Map<?, ?>) result).size();
    }

    log.info("预热完成: {} | 数据量: {} | 耗时: {}ms", task.getDescription(), count, duration);
  }

  /**
   * 异步执行预热任务.
   *
   * @param task 预热任务
   */
  @Async
  public void executeAsync(final WarmUpTask task) {
    try {
      executeSync(task);
    } catch (Exception e) {
      log.error("异步预热失败: {}", task.getDescription(), e);
    }
  }

  /** 预热任务. */
  @lombok.Data
  private static class WarmUpTask {
    /** Bean实例. */
    private Object bean;

    /** 方法. */
    private Method method;

    /** 缓存键前缀. */
    private String keyPrefix;

    /** 执行顺序. */
    private int order;

    /** 任务描述. */
    private String description;

    /** 是否异步执行. */
    private boolean async;

    /** 失败时是否继续. */
    private boolean continueOnError;
  }
}
