package com.lawfirm.common.resilience;

/**
 * 熔断器打开异常
 * 
 * 当熔断器处于 OPEN 状态时，请求会被拒绝并抛出此异常
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
public class CircuitBreakerOpenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CircuitBreakerOpenException(String message) {
        super(message);
    }

    public CircuitBreakerOpenException(String message, Throwable cause) {
        super(message, cause);
    }
}

