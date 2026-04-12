package com.archivesystem.common.exception;

import com.archivesystem.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void testHandleBusinessException() {
        BusinessException exception = new BusinessException("500", "业务处理失败");

        Result<Void> result = globalExceptionHandler.handleBusinessException(exception);

        assertEquals("500", result.getCode());
        assertEquals("业务处理失败", result.getMessage());
    }

    @Test
    void testHandleBusinessException_WithDefaultCode() {
        BusinessException exception = new BusinessException("操作失败");

        Result<Void> result = globalExceptionHandler.handleBusinessException(exception);

        assertEquals("500", result.getCode());
        assertEquals("操作失败", result.getMessage());
    }

    @Test
    void testHandleUnauthorizedException() {
        UnauthorizedException exception = new UnauthorizedException("未登录或登录已过期");

        Result<Void> result = globalExceptionHandler.handleUnauthorizedException(exception);

        assertEquals("401", result.getCode());
        assertEquals("未登录或登录已过期", result.getMessage());
    }

    @Test
    void testHandleForbiddenException() {
        ForbiddenException exception = new ForbiddenException("没有访问权限");

        Result<Void> result = globalExceptionHandler.handleForbiddenException(exception);

        assertEquals("403", result.getCode());
        assertEquals("没有访问权限", result.getMessage());
    }

    @Test
    void testHandleAccessDeniedException() {
        AccessDeniedException exception = new AccessDeniedException("Access Denied");

        Result<Void> result = globalExceptionHandler.handleAccessDeniedException(exception);

        assertEquals("403", result.getCode());
        assertEquals("没有访问权限", result.getMessage());
    }

    @Test
    void testHandleNotFoundException() {
        NotFoundException exception = new NotFoundException("资源不存在");

        Result<Void> result = globalExceptionHandler.handleNotFoundException(exception);

        assertEquals("404", result.getCode());
        assertEquals("资源不存在", result.getMessage());
    }

    @Test
    void testHandleValidationException() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("object", "field1", "字段1不能为空");
        FieldError fieldError2 = new FieldError("object", "field2", "字段2格式错误");

        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        Result<Void> result = globalExceptionHandler.handleValidationException(exception);

        assertEquals("400", result.getCode());
        assertTrue(result.getMessage().contains("字段1不能为空"));
        assertTrue(result.getMessage().contains("字段2格式错误"));
    }

    @Test
    void testHandleBindException() {
        BindException exception = mock(BindException.class);
        FieldError fieldError = new FieldError("object", "name", "名称不能为空");

        when(exception.getFieldErrors()).thenReturn(Arrays.asList(fieldError));

        Result<Void> result = globalExceptionHandler.handleBindException(exception);

        assertEquals("400", result.getCode());
        assertEquals("名称不能为空", result.getMessage());
    }

    @Test
    void testHandleConstraintViolationException() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();

        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        when(violation1.getMessage()).thenReturn("参数不能为空");

        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        when(violation2.getMessage()).thenReturn("参数格式不正确");

        violations.add(violation1);
        violations.add(violation2);

        ConstraintViolationException exception = new ConstraintViolationException(violations);

        Result<Void> result = globalExceptionHandler.handleConstraintViolationException(exception);

        assertEquals("400", result.getCode());
        assertNotNull(result.getMessage());
    }

    @Test
    void testHandleException() {
        Exception exception = new RuntimeException("未知错误");

        Result<Void> result = globalExceptionHandler.handleException(exception);

        assertEquals("500", result.getCode());
        assertEquals("系统内部错误，请稍后重试", result.getMessage());
    }

    @Test
    void testHandleException_NullPointerException() {
        NullPointerException exception = new NullPointerException();

        Result<Void> result = globalExceptionHandler.handleException(exception);

        assertEquals("500", result.getCode());
        assertEquals("系统内部错误，请稍后重试", result.getMessage());
    }
}
