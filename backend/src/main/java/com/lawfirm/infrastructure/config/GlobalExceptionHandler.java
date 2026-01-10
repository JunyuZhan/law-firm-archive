package com.lawfirm.infrastructure.config;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 * 生产环境不返回详细错误信息，避免泄露敏感信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final Environment environment;
    
    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * 判断是否为生产环境
     */
    private boolean isProduction() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
    
    /**
     * 获取安全的错误消息（生产环境不返回详细错误）
     */
    private String getSafeErrorMessage(String detailedMessage, String defaultMessage) {
        if (isProduction()) {
            return defaultMessage;
        }
        return detailedMessage;
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, jakarta.servlet.http.HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        
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
     * 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 绑定异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数绑定失败: {}", message);
        return Result.badRequest(message);
    }

    /**
     * 其他异常
     * 生产环境不返回详细错误信息，避免泄露系统内部结构
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        // 记录完整异常信息到日志（包含堆栈）
        log.error("系统异常: ", e);
        
        // 生产环境返回通用错误信息，开发环境可以返回更详细的信息
        String errorMessage = isProduction() 
            ? "系统繁忙，请稍后重试" 
            : getSafeErrorMessage(e.getMessage(), "系统异常: " + e.getClass().getSimpleName());
        
        return Result.error(errorMessage);
    }
}

