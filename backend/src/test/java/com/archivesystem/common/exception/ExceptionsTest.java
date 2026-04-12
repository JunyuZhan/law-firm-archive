package com.archivesystem.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ExceptionsTest {

    // BusinessException Tests
    @Test
    void testBusinessException_MessageOnly() {
        BusinessException exception = new BusinessException("操作失败");

        assertEquals("500", exception.getCode());
        assertEquals("操作失败", exception.getMessage());
    }

    @Test
    void testBusinessException_CodeAndMessage() {
        BusinessException exception = new BusinessException("400", "参数错误");

        assertEquals("400", exception.getCode());
        assertEquals("参数错误", exception.getMessage());
    }

    @Test
    void testBusinessException_WithCause() {
        Throwable cause = new RuntimeException("底层错误");
        BusinessException exception = new BusinessException("500", "处理失败", cause);

        assertEquals("500", exception.getCode());
        assertEquals("处理失败", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    // UnauthorizedException Tests
    @Test
    void testUnauthorizedException() {
        UnauthorizedException exception = new UnauthorizedException("未授权访问");

        assertEquals("未授权访问", exception.getMessage());
    }

    // ForbiddenException Tests
    @Test
    void testForbiddenException() {
        ForbiddenException exception = new ForbiddenException("禁止访问");

        assertEquals("禁止访问", exception.getMessage());
    }

    // NotFoundException Tests
    @Test
    void testNotFoundException() {
        NotFoundException exception = new NotFoundException("资源不存在");

        assertEquals("资源不存在", exception.getMessage());
    }

    // All exceptions should extend RuntimeException
    @Test
    void testBusinessException_IsRuntimeException() {
        BusinessException exception = new BusinessException("test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testUnauthorizedException_IsRuntimeException() {
        UnauthorizedException exception = new UnauthorizedException("test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testForbiddenException_IsRuntimeException() {
        ForbiddenException exception = new ForbiddenException("test");
        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    void testNotFoundException_IsRuntimeException() {
        NotFoundException exception = new NotFoundException("test");
        assertTrue(exception instanceof RuntimeException);
    }
}
