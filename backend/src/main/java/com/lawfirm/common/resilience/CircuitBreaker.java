package com.lawfirm.common.resilience;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

/**
 * 熔断器实现
 *
 * <p>功能： - 三种状态：CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（试探） - 失败次数达到阈值自动熔断 - 熔断超时后自动半开试探 - 半开成功后自动恢复
 *
 * <p>使用场景： - Redis 缓存操作 - 外部 API 调用 - 消息发送
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public class CircuitBreaker {

  /** 熔断器状态 */
  public enum State {
    /** 正常状态，所有请求正常执行 */
    CLOSED,
    /** 熔断状态，所有请求直接失败或使用降级 */
    OPEN,
    /** 半开状态，允许少量请求试探 */
    HALF_OPEN
  }

  /** 熔断器名称 */
  private final String name;

  /** 失败阈值，超过此值触发熔断 */
  private final int failureThreshold;

  /** 熔断超时时间（毫秒），超时后进入半开状态 */
  private final long timeoutMs;

  /** 半开状态成功阈值，达到后恢复正常 */
  private final int halfOpenSuccessThreshold;

  /** 当前状态 */
  private volatile State state = State.CLOSED;

  /** 失败计数 */
  private final AtomicInteger failureCount = new AtomicInteger(0);

  /** 半开状态成功计数 */
  private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);

  /** 最后失败时间 */
  private final AtomicLong lastFailureTime = new AtomicLong(0);

  /** 全局熔断器注册表 */
  private static final ConcurrentHashMap<String, CircuitBreaker> REGISTRY =
      new ConcurrentHashMap<>();

  /** 默认熔断超时时间（毫秒）：60秒 */
  private static final long DEFAULT_TIMEOUT_MS = 60000L;

  /**
   * 构造函数
   *
   * @param name 熔断器名称
   * @param failureThreshold 失败阈值
   * @param timeoutMs 熔断超时时间（毫秒）
   * @param halfOpenSuccessThreshold 半开成功阈值
   */
  public CircuitBreaker(
      String name, int failureThreshold, long timeoutMs, int halfOpenSuccessThreshold) {
    this.name = name;
    this.failureThreshold = failureThreshold;
    this.timeoutMs = timeoutMs;
    this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
  }

  /**
   * 使用默认配置创建熔断器
   *
   * @param name 熔断器名称
   */
  public CircuitBreaker(String name) {
    this(name, 5, DEFAULT_TIMEOUT_MS, 3); // 默认：5次失败熔断，60秒超时，3次成功恢复
  }

  /**
   * 获取或创建熔断器实例
   *
   * @param name 熔断器名称
   * @return 熔断器实例
   */
  public static CircuitBreaker of(final String name) {
    return REGISTRY.computeIfAbsent(name, CircuitBreaker::new);
  }

  /**
   * 获取或创建熔断器实例（自定义配置）
   *
   * @param name 熔断器名称
   * @param failureThreshold 失败阈值
   * @param timeoutMs 超时时间（毫秒）
   * @param halfOpenSuccessThreshold 半开状态成功阈值
   * @return 熔断器实例
   */
  public static CircuitBreaker of(
      final String name,
      final int failureThreshold,
      final long timeoutMs,
      final int halfOpenSuccessThreshold) {
    return REGISTRY.computeIfAbsent(
        name, n -> new CircuitBreaker(n, failureThreshold, timeoutMs, halfOpenSuccessThreshold));
  }

  /**
   * 执行操作（带熔断保护）
   *
   * @param supplier 操作
   * @param <T> 返回类型
   * @return 操作结果
   * @throws CircuitBreakerOpenException 熔断器打开时抛出
   */
  public <T> T execute(final Supplier<T> supplier) {
    if (!isAllowed()) {
      log.warn("CircuitBreaker [{}] is OPEN, rejecting request", name);
      throw new CircuitBreakerOpenException("熔断器已打开: " + name);
    }

    try {
      T result = supplier.get();
      recordSuccess();
      return result;
    } catch (Exception e) {
      recordFailure();
      throw e;
    }
  }

  /**
   * 执行操作（带降级）
   *
   * @param supplier 正常操作
   * @param fallback 降级操作
   * @param <T> 返回类型
   * @return 操作结果
   */
  public <T> T executeWithFallback(final Supplier<T> supplier, final Supplier<T> fallback) {
    if (!isAllowed()) {
      log.warn("CircuitBreaker [{}] is OPEN, using fallback", name);
      return fallback.get();
    }

    try {
      T result = supplier.get();
      recordSuccess();
      return result;
    } catch (Exception e) {
      recordFailure();
      log.warn("CircuitBreaker [{}] execution failed, using fallback: {}", name, e.getMessage());
      return fallback.get();
    }
  }

  /**
   * 执行无返回值操作
   *
   * @param runnable 操作
   */
  public void execute(final Runnable runnable) {
    execute(
        () -> {
          runnable.run();
          return null;
        });
  }

  /**
   * 检查是否允许请求通过
   *
   * @return 如果允许请求通过返回true，否则返回false
   */
  public boolean isAllowed() {
    if (state == State.CLOSED) {
      return true;
    }

    if (state == State.OPEN) {
      // 检查是否超时，超时则进入半开状态
      if (System.currentTimeMillis() - lastFailureTime.get() > timeoutMs) {
        log.info("CircuitBreaker [{}] timeout, transitioning to HALF_OPEN", name);
        state = State.HALF_OPEN;
        halfOpenSuccessCount.set(0);
        return true;
      }
      return false;
    }

    // HALF_OPEN 状态，允许请求通过
    return true;
  }

  /** 记录成功 */
  private void recordSuccess() {
    if (state == State.HALF_OPEN) {
      int successCount = halfOpenSuccessCount.incrementAndGet();
      if (successCount >= halfOpenSuccessThreshold) {
        log.info("CircuitBreaker [{}] recovered, transitioning to CLOSED", name);
        reset();
      }
    } else if (state == State.CLOSED) {
      // 成功时重置失败计数
      failureCount.set(0);
    }
  }

  /** 记录失败 */
  private void recordFailure() {
    lastFailureTime.set(System.currentTimeMillis());

    if (state == State.HALF_OPEN) {
      // 半开状态失败，重新打开熔断器
      log.warn("CircuitBreaker [{}] failed in HALF_OPEN, transitioning to OPEN", name);
      state = State.OPEN;
      halfOpenSuccessCount.set(0);
    } else if (state == State.CLOSED) {
      int failures = failureCount.incrementAndGet();
      if (failures >= failureThreshold) {
        log.warn(
            "CircuitBreaker [{}] failure threshold reached ({}), transitioning to OPEN",
            name,
            failures);
        state = State.OPEN;
      }
    }
  }

  /** 重置熔断器 */
  public void reset() {
    state = State.CLOSED;
    failureCount.set(0);
    halfOpenSuccessCount.set(0);
    lastFailureTime.set(0);
  }

  /**
   * 获取当前状态
   *
   * @return 当前熔断器状态
   */
  public State getState() {
    return state;
  }

  /**
   * 获取失败计数
   *
   * @return 当前失败次数
   */
  public int getFailureCount() {
    return failureCount.get();
  }

  /**
   * 获取熔断器名称
   *
   * @return 熔断器名称
   */
  public String getName() {
    return name;
  }

  /** 手动打开熔断器 */
  public void trip() {
    state = State.OPEN;
    lastFailureTime.set(System.currentTimeMillis());
    log.warn("CircuitBreaker [{}] manually tripped", name);
  }

  /**
   * 获取所有熔断器状态
   *
   * @return 所有熔断器的映射表
   */
  public static ConcurrentHashMap<String, CircuitBreaker> getAllBreakers() {
    return new ConcurrentHashMap<>(REGISTRY);
  }

  /** 清除所有熔断器 */
  public static void clearAll() {
    REGISTRY.clear();
  }

  @Override
  public String toString() {
    return String.format(
        "CircuitBreaker[name=%s, state=%s, failures=%d]", name, state, failureCount.get());
  }
}
