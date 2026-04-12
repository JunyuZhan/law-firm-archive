package com.archivesystem.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class ErrorCodeTest {

    @Test
    void testSuccessCode() {
        assertEquals("200", ErrorCode.SUCCESS.getCode());
        assertEquals("成功", ErrorCode.SUCCESS.getMessage());
    }

    @Test
    void testBadRequestCode() {
        assertEquals("400", ErrorCode.BAD_REQUEST.getCode());
        assertEquals("请求参数错误", ErrorCode.BAD_REQUEST.getMessage());
    }

    @Test
    void testUnauthorizedCode() {
        assertEquals("401", ErrorCode.UNAUTHORIZED.getCode());
        assertEquals("未登录或登录已过期", ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    void testForbiddenCode() {
        assertEquals("403", ErrorCode.FORBIDDEN.getCode());
        assertEquals("无权限访问", ErrorCode.FORBIDDEN.getMessage());
    }

    @Test
    void testNotFoundCode() {
        assertEquals("404", ErrorCode.NOT_FOUND.getCode());
        assertEquals("资源不存在", ErrorCode.NOT_FOUND.getMessage());
    }

    @Test
    void testInternalErrorCode() {
        assertEquals("500", ErrorCode.INTERNAL_ERROR.getCode());
        assertEquals("系统内部错误", ErrorCode.INTERNAL_ERROR.getMessage());
    }

    // 认证相关错误码
    @Test
    void testAuthInvalidTokenCode() {
        assertEquals("1001", ErrorCode.AUTH_INVALID_TOKEN.getCode());
        assertEquals("无效的Token", ErrorCode.AUTH_INVALID_TOKEN.getMessage());
    }

    @Test
    void testAuthTokenExpiredCode() {
        assertEquals("1002", ErrorCode.AUTH_TOKEN_EXPIRED.getCode());
        assertEquals("Token已过期", ErrorCode.AUTH_TOKEN_EXPIRED.getMessage());
    }

    @Test
    void testAuthUserDisabledCode() {
        assertEquals("1003", ErrorCode.AUTH_USER_DISABLED.getCode());
        assertEquals("用户已被禁用", ErrorCode.AUTH_USER_DISABLED.getMessage());
    }

    @Test
    void testAuthPasswordErrorCode() {
        assertEquals("1004", ErrorCode.AUTH_PASSWORD_ERROR.getCode());
        assertEquals("密码错误", ErrorCode.AUTH_PASSWORD_ERROR.getMessage());
    }

    // 档案相关错误码
    @Test
    void testArchiveNotFoundCode() {
        assertEquals("2001", ErrorCode.ARCHIVE_NOT_FOUND.getCode());
        assertEquals("档案不存在", ErrorCode.ARCHIVE_NOT_FOUND.getMessage());
    }

    @Test
    void testArchiveNoExistsCode() {
        assertEquals("2002", ErrorCode.ARCHIVE_NO_EXISTS.getCode());
        assertEquals("档案号已存在", ErrorCode.ARCHIVE_NO_EXISTS.getMessage());
    }

    @Test
    void testArchiveStatusErrorCode() {
        assertEquals("2003", ErrorCode.ARCHIVE_STATUS_ERROR.getCode());
        assertEquals("档案状态不允许此操作", ErrorCode.ARCHIVE_STATUS_ERROR.getMessage());
    }

    @Test
    void testArchiveFileNotFoundCode() {
        assertEquals("2004", ErrorCode.ARCHIVE_FILE_NOT_FOUND.getCode());
        assertEquals("档案文件不存在", ErrorCode.ARCHIVE_FILE_NOT_FOUND.getMessage());
    }

    // 借阅相关错误码
    @Test
    void testBorrowNotFoundCode() {
        assertEquals("3001", ErrorCode.BORROW_NOT_FOUND.getCode());
        assertEquals("借阅申请不存在", ErrorCode.BORROW_NOT_FOUND.getMessage());
    }

    @Test
    void testBorrowAlreadyExistsCode() {
        assertEquals("3002", ErrorCode.BORROW_ALREADY_EXISTS.getCode());
        assertEquals("该档案已有待处理的借阅申请", ErrorCode.BORROW_ALREADY_EXISTS.getMessage());
    }

    @Test
    void testBorrowStatusErrorCode() {
        assertEquals("3003", ErrorCode.BORROW_STATUS_ERROR.getCode());
        assertEquals("借阅状态不允许此操作", ErrorCode.BORROW_STATUS_ERROR.getMessage());
    }

    // 文件相关错误码
    @Test
    void testFileUploadErrorCode() {
        assertEquals("4001", ErrorCode.FILE_UPLOAD_ERROR.getCode());
        assertEquals("文件上传失败", ErrorCode.FILE_UPLOAD_ERROR.getMessage());
    }

    @Test
    void testFileDownloadErrorCode() {
        assertEquals("4002", ErrorCode.FILE_DOWNLOAD_ERROR.getCode());
        assertEquals("文件下载失败", ErrorCode.FILE_DOWNLOAD_ERROR.getMessage());
    }

    @Test
    void testFileTypeNotAllowedCode() {
        assertEquals("4003", ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode());
        assertEquals("不支持的文件类型", ErrorCode.FILE_TYPE_NOT_ALLOWED.getMessage());
    }

    @Test
    void testFileSizeExceededCode() {
        assertEquals("4004", ErrorCode.FILE_SIZE_EXCEEDED.getCode());
        assertEquals("文件大小超出限制", ErrorCode.FILE_SIZE_EXCEEDED.getMessage());
    }

    @Test
    void testEnumValuesCount() {
        // 验证枚举值数量
        assertEquals(21, ErrorCode.values().length);
    }

    @Test
    void testValueOf() {
        assertEquals(ErrorCode.SUCCESS, ErrorCode.valueOf("SUCCESS"));
        assertEquals(ErrorCode.ARCHIVE_NOT_FOUND, ErrorCode.valueOf("ARCHIVE_NOT_FOUND"));
    }
}
