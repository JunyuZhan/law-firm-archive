package com.lawfirm.common.resilience;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CircuitBreaker 单元测试
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@DisplayName("CircuitBreaker 单元测试")
class CircuitBreakerTest {

  private CircuitBreaker circuitBreaker;

  @BeforeEach
  void setUp() {
    CircuitBreaker.clearAll();
    // 2次失败熔断，100ms超时，1次成功恢复（便于测试）
    circuitBreaker = new CircuitBreaker("test", 2, 100, 1);
  }

  @Nested
  @DisplayName("状态转换测试")
  class StateTransitionTests {

    @Test
    @DisplayName("初始状态应为 CLOSED")
    void initialState_ShouldBeClosed() {
      assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("失败达到阈值后应转为 OPEN")
    void afterFailureThreshold_ShouldBeOpen() {
      // Given & When
      for (int i = 0; i < 2; i++) {
        try {
          circuitBreaker.execute(
              () -> {
                throw new RuntimeException("Test failure");
              });
        } catch (Exception ignored) {
        }
      }

      // Then
      assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("熔断超时后应转为 HALF_OPEN")
    void afterTimeout_ShouldBeHalfOpen() throws InterruptedException {
      // Given - 触发熔断
      for (int i = 0; i < 2; i++) {
        try {
          circuitBreaker.execute(
              () -> {
                throw new RuntimeException("Test failure");
              });
        } catch (Exception ignored) {
        }
      }
      assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

      // When - 等待超时
      Thread.sleep(150);
      circuitBreaker.isAllowed();

      // Then
      assertEquals(CircuitBreaker.State.HALF_OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("半开状态成功后应恢复为 CLOSED")
    void halfOpenSuccess_ShouldBeClosed() throws InterruptedException {
      // Given - 触发熔断后等待超时
      for (int i = 0; i < 2; i++) {
        try {
          circuitBreaker.execute(
              () -> {
                throw new RuntimeException("Test failure");
              });
        } catch (Exception ignored) {
        }
      }
      Thread.sleep(150);
      circuitBreaker.isAllowed(); // 触发进入 HALF_OPEN

      // When - 执行成功
      String result = circuitBreaker.execute(() -> "success");

      // Then
      assertEquals("success", result);
      assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("半开状态失败后应重新打开")
    void halfOpenFailure_ShouldBeOpen() throws InterruptedException {
      // Given - 触发熔断后等待超时
      for (int i = 0; i < 2; i++) {
        try {
          circuitBreaker.execute(
              () -> {
                throw new RuntimeException("Test failure");
              });
        } catch (Exception ignored) {
        }
      }
      Thread.sleep(150);
      circuitBreaker.isAllowed(); // 触发进入 HALF_OPEN

      // When - 执行失败
      try {
        circuitBreaker.execute(
            () -> {
              throw new RuntimeException("Test failure again");
            });
      } catch (Exception ignored) {
      }

      // Then
      assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }
  }

  @Nested
  @DisplayName("执行测试")
  class ExecutionTests {

    @Test
    @DisplayName("正常执行应返回结果")
    void execute_Success_ShouldReturnResult() {
      // When
      String result = circuitBreaker.execute(() -> "hello");

      // Then
      assertEquals("hello", result);
      assertEquals(0, circuitBreaker.getFailureCount());
    }

    @Test
    @DisplayName("执行失败应抛出原异常")
    void execute_Failure_ShouldThrowOriginalException() {
      // When & Then
      RuntimeException exception =
          assertThrows(
              RuntimeException.class,
              () ->
                  circuitBreaker.execute(
                      () -> {
                        throw new IllegalArgumentException("Invalid argument");
                      }));
      assertEquals("Invalid argument", exception.getMessage());
    }

    @Test
    @DisplayName("熔断状态应抛出 CircuitBreakerOpenException")
    void execute_WhenOpen_ShouldThrowCircuitBreakerException() {
      // Given - 触发熔断
      for (int i = 0; i < 2; i++) {
        try {
          circuitBreaker.execute(
              () -> {
                throw new RuntimeException("Test failure");
              });
        } catch (Exception ignored) {
        }
      }

      // When & Then
      assertThrows(
          CircuitBreakerOpenException.class,
          () -> circuitBreaker.execute(() -> "should not execute"));
    }
  }

  @Nested
  @DisplayName("降级测试")
  class FallbackTests {

    @Test
    @DisplayName("正常执行不应使用降级")
    void executeWithFallback_Success_ShouldNotUseFallback() {
      // Given
      AtomicInteger fallbackCalled = new AtomicInteger(0);

      // When
      String result =
          circuitBreaker.executeWithFallback(
              () -> "primary",
              () -> {
                fallbackCalled.incrementAndGet();
                return "fallback";
              });

      // Then
      assertEquals("primary", result);
      assertEquals(0, fallbackCalled.get());
    }

    @Test
    @DisplayName("执行失败应使用降级")
    void executeWithFallback_Failure_ShouldUseFallback() {
      // When
      String result =
          circuitBreaker.executeWithFallback(
              () -> {
                throw new RuntimeException("Primary failed");
              },
              () -> "fallback");

      // Then
      assertEquals("fallback", result);
    }

    @Test
    @DisplayName("熔断状态应直接使用降级")
    void executeWithFallback_WhenOpen_ShouldUseFallback() {
      // Given - 触发熔断
      for (int i = 0; i < 2; i++) {
        circuitBreaker.executeWithFallback(
            () -> {
              throw new RuntimeException("Test failure");
            },
            () -> "fallback");
      }

      // When
      AtomicInteger primaryCalled = new AtomicInteger(0);
      String result =
          circuitBreaker.executeWithFallback(
              () -> {
                primaryCalled.incrementAndGet();
                return "primary";
              },
              () -> "fallback");

      // Then
      assertEquals("fallback", result);
      assertEquals(0, primaryCalled.get()); // 主逻辑不应被调用
    }
  }

  @Nested
  @DisplayName("手动控制测试")
  class ManualControlTests {

    @Test
    @DisplayName("手动打开熔断器")
    void trip_ShouldOpenCircuitBreaker() {
      // When
      circuitBreaker.trip();

      // Then
      assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("重置熔断器")
    void reset_ShouldCloseCircuitBreaker() {
      // Given
      circuitBreaker.trip();
      assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

      // When
      circuitBreaker.reset();

      // Then
      assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
      assertEquals(0, circuitBreaker.getFailureCount());
    }
  }

  @Nested
  @DisplayName("注册表测试")
  class RegistryTests {

    @Test
    @DisplayName("相同名称应返回相同实例")
    void of_SameName_ShouldReturnSameInstance() {
      // When
      CircuitBreaker cb1 = CircuitBreaker.of("shared");
      CircuitBreaker cb2 = CircuitBreaker.of("shared");

      // Then
      assertSame(cb1, cb2);
    }

    @Test
    @DisplayName("不同名称应返回不同实例")
    void of_DifferentNames_ShouldReturnDifferentInstances() {
      // When
      CircuitBreaker cb1 = CircuitBreaker.of("first");
      CircuitBreaker cb2 = CircuitBreaker.of("second");

      // Then
      assertNotSame(cb1, cb2);
    }
  }
}
