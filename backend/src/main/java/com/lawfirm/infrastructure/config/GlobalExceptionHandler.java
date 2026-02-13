package com.lawfirm.infrastructure.config;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.Result;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理.
 *
 * <p>生产环境不返回详细错误信息，避免泄露敏感信息
 *
 * @author system
 * @since 2026-01-17
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** 日志记录器 */
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Spring环境配置. */
  private final Environment environment;

  /**
   * 构造函数.
   *
   * @param environment 环境配置
   */
  public GlobalExceptionHandler(final Environment environment) {
    this.environment = environment;
  }

  /**
   * 判断是否为生产环境.
   *
   * @return 是否为生产环境
   */
  private boolean isProduction() {
    return Arrays.asList(environment.getActiveProfiles()).contains("prod");
  }

  /**
   * 获取安全的错误消息（生产环境不返回详细错误）.
   *
   * @param detailedMessage 详细错误消息
   * @param defaultMessage 默认错误消息
   * @return 安全的错误消息
   */
  private String getSafeErrorMessage(final String detailedMessage, final String defaultMessage) {
    if (isProduction()) {
      return defaultMessage;
    }
    return detailedMessage;
  }

  /**
   * 业务异常处理.
   *
   * @param e 业务异常
   * @param response HTTP响应
   * @return 错误结果
   */
  @ExceptionHandler(BusinessException.class)
  public Result<Void> handleBusinessException(
      final BusinessException e, final jakarta.servlet.http.HttpServletResponse response) {
    LOG.warn("业务异常: {}", e.getMessage());

    // 根据错误码设置对应的HTTP状态码
    String code = e.getCode();
    if ("401".equals(code)) {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
    } else if ("403".equals(code)) {
      response.setStatus(HttpStatus.FORBIDDEN.value());
    } else if ("404".equals(code)) {
      response.setStatus(HttpStatus.NOT_FOUND.value());
    } else if ("400".equals(code)) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
    } else {
      response.setStatus(HttpStatus.OK.value());
    }

    return Result.error(e.getCode(), e.getMessage());
  }

  /**
   * 参数校验异常处理.
   *
   * @param e 参数校验异常
   * @return 错误结果
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> handleValidationException(final MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    LOG.warn("参数校验失败: {}", message);
    return Result.badRequest(message);
  }

  /**
   * 绑定异常处理.
   *
   * @param e 绑定异常
   * @return 错误结果
   */
  @ExceptionHandler(BindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Result<Void> handleBindException(final BindException e) {
    String message =
        e.getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
    LOG.warn("参数绑定失败: {}", message);
    return Result.badRequest(message);
  }

  /**
   * 其他异常处理.
   *
   * <p>生产环境不返回详细错误信息，避免泄露系统内部结构
   *
   * @param e 异常
   * @return 错误结果
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public Result<Void> handleException(final Exception e) {
    // 记录完整异常信息到日志（包含堆栈）
    LOG.error("系统异常: ", e);

    // 生产环境返回通用错误信息，开发环境可以返回更详细的信息
    String errorMessage =
        isProduction()
            ? "系统繁忙，请稍后重试"
            : getSafeErrorMessage(e.getMessage(), "系统异常: " + e.getClass().getSimpleName());

    return Result.error(errorMessage);
  }
}
