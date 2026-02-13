package com.lawfirm.common.resilience;

/**
 * 熔断器打开异常
 *
 * <p>当熔断器处于 OPEN 状态时，请求会被拒绝并抛出此异常
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
public class CircuitBreakerOpenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * 构造函数
   *
   * @param message 异常消息
   */
  public CircuitBreakerOpenException(final String message) {
    super(message);
  }

  /**
   * 构造函数
   *
   * @param message 异常消息
   * @param cause 原因
   */
  public CircuitBreakerOpenException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
