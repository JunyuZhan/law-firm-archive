package com.archivesystem.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举.
 * @author junyuzhan
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 通用错误
    SUCCESS("200", "成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未登录或登录已过期"),
    FORBIDDEN("403", "无权限访问"),
    NOT_FOUND("404", "资源不存在"),
    INTERNAL_ERROR("500", "系统内部错误"),

    // 认证相关 1xxx
    AUTH_INVALID_TOKEN("1001", "无效的Token"),
    AUTH_TOKEN_EXPIRED("1002", "Token已过期"),
    AUTH_USER_DISABLED("1003", "用户已被禁用"),
    AUTH_PASSWORD_ERROR("1004", "密码错误"),

    // 档案相关 2xxx
    ARCHIVE_NOT_FOUND("2001", "档案不存在"),
    ARCHIVE_NO_EXISTS("2002", "档案号已存在"),
    ARCHIVE_STATUS_ERROR("2003", "档案状态不允许此操作"),
    ARCHIVE_FILE_NOT_FOUND("2004", "档案文件不存在"),

    // 借阅相关 3xxx
    BORROW_NOT_FOUND("3001", "借阅申请不存在"),
    BORROW_ALREADY_EXISTS("3002", "该档案已有待处理的借阅申请"),
    BORROW_STATUS_ERROR("3003", "借阅状态不允许此操作"),

    // 文件相关 4xxx
    FILE_UPLOAD_ERROR("4001", "文件上传失败"),
    FILE_DOWNLOAD_ERROR("4002", "文件下载失败"),
    FILE_TYPE_NOT_ALLOWED("4003", "不支持的文件类型"),
    FILE_SIZE_EXCEEDED("4004", "文件大小超出限制");

    private final String code;
    private final String message;
}
