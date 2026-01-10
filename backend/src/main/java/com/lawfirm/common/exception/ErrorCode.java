package com.lawfirm.common.exception;

import lombok.Getter;

/**
 * 统一错误码枚举
 * 
 * 错误码规则：
 * - 1xxx: 认证授权相关
 * - 2xxx: 业务逻辑相关
 * - 3xxx: 参数校验相关
 * - 4xxx: 数据操作相关
 * - 5xxx: 系统内部错误
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
@Getter
public enum ErrorCode {

    // ==================== 成功 ====================
    SUCCESS("0", "操作成功"),

    // ==================== 1xxx 认证授权 ====================
    UNAUTHORIZED("1001", "未登录或登录已过期"),
    TOKEN_EXPIRED("1002", "Token已过期"),
    TOKEN_INVALID("1003", "Token无效"),
    ACCESS_DENIED("1004", "无权限访问"),
    ACCOUNT_LOCKED("1005", "账户已被锁定"),
    ACCOUNT_DISABLED("1006", "账户已被禁用"),
    LOGIN_FAILED("1007", "用户名或密码错误"),
    CAPTCHA_ERROR("1008", "验证码错误"),
    CAPTCHA_EXPIRED("1009", "验证码已过期"),
    TOO_MANY_ATTEMPTS("1010", "登录尝试次数过多，请稍后重试"),

    // ==================== 2xxx 业务逻辑 ====================
    // 客户相关 20xx
    CLIENT_NOT_FOUND("2001", "客户不存在"),
    CLIENT_NAME_EXISTS("2002", "客户名称已存在"),
    CLIENT_HAS_MATTERS("2003", "客户关联项目，无法删除"),

    // 合同相关 21xx
    CONTRACT_NOT_FOUND("2101", "合同不存在"),
    CONTRACT_NOT_APPROVED("2102", "合同未审批通过"),
    CONTRACT_EXPIRED("2103", "合同已过期"),
    CONTRACT_AMOUNT_EXCEEDED("2104", "超出合同金额"),

    // 项目相关 22xx
    MATTER_NOT_FOUND("2201", "项目不存在"),
    MATTER_NO_EXISTS("2202", "项目编号已存在"),
    MATTER_CLOSED("2203", "项目已结案"),
    MATTER_NO_CONTRACT("2204", "项目必须关联合同"),
    MATTER_HAS_UNPAID_FEE("2205", "项目存在未收款项"),

    // 财务相关 23xx
    FEE_NOT_FOUND("2301", "收费记录不存在"),
    PAYMENT_NOT_FOUND("2302", "收款记录不存在"),
    INVOICE_NOT_FOUND("2303", "发票不存在"),
    AMOUNT_INVALID("2304", "金额无效"),
    PAYMENT_EXCEED_FEE("2305", "收款金额超过应收金额"),

    // 文档相关 24xx
    DOCUMENT_NOT_FOUND("2401", "文档不存在"),
    DOCUMENT_UPLOAD_FAILED("2402", "文档上传失败"),
    DOCUMENT_FORMAT_INVALID("2403", "文档格式不支持"),
    DOCUMENT_SIZE_EXCEEDED("2404", "文档大小超出限制"),

    // 证据相关 25xx
    EVIDENCE_NOT_FOUND("2501", "证据不存在"),
    EVIDENCE_LIST_NOT_FOUND("2502", "证据清单不存在"),

    // 用户相关 26xx
    USER_NOT_FOUND("2601", "用户不存在"),
    USER_EXISTS("2602", "用户已存在"),
    PASSWORD_INCORRECT("2603", "密码不正确"),
    OLD_PASSWORD_INCORRECT("2604", "旧密码不正确"),

    // ==================== 3xxx 参数校验 ====================
    PARAM_INVALID("3001", "参数无效"),
    PARAM_MISSING("3002", "缺少必要参数"),
    PARAM_TYPE_ERROR("3003", "参数类型错误"),
    PARAM_FORMAT_ERROR("3004", "参数格式错误"),
    PHONE_FORMAT_ERROR("3005", "手机号格式不正确"),
    IDCARD_FORMAT_ERROR("3006", "身份证号格式不正确"),
    EMAIL_FORMAT_ERROR("3007", "邮箱格式不正确"),
    DATE_FORMAT_ERROR("3008", "日期格式不正确"),

    // ==================== 4xxx 数据操作 ====================
    DATA_NOT_FOUND("4001", "数据不存在"),
    DATA_EXISTS("4002", "数据已存在"),
    DATA_CONFLICT("4003", "数据冲突"),
    DATA_INTEGRITY_ERROR("4004", "数据完整性错误"),
    OPTIMISTIC_LOCK_ERROR("4005", "数据已被修改，请刷新后重试"),
    FOREIGN_KEY_ERROR("4006", "存在关联数据，无法删除"),

    // ==================== 5xxx 系统错误 ====================
    SYSTEM_ERROR("5000", "系统错误，请稍后重试"),
    SERVICE_UNAVAILABLE("5001", "服务暂不可用"),
    DATABASE_ERROR("5002", "数据库错误"),
    REDIS_ERROR("5003", "缓存服务错误"),
    FILE_STORAGE_ERROR("5004", "文件存储错误"),
    NETWORK_ERROR("5005", "网络错误"),
    TIMEOUT_ERROR("5006", "请求超时"),
    RATE_LIMIT_EXCEEDED("5007", "请求过于频繁"),
    REPEAT_SUBMIT("5008", "请勿重复提交"),
    CIRCUIT_BREAKER_OPEN("5009", "服务熔断中，请稍后重试");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误消息
     */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 创建业务异常
     */
    public BusinessException toException() {
        return new BusinessException(this.code, this.message);
    }

    /**
     * 创建业务异常（自定义消息）
     */
    public BusinessException toException(String customMessage) {
        return new BusinessException(this.code, customMessage);
    }

    /**
     * 创建业务异常（格式化消息）
     */
    public BusinessException toException(Object... args) {
        String formattedMessage = String.format(this.message.replace("{}", "%s"), args);
        return new BusinessException(this.code, formattedMessage);
    }

    /**
     * 根据code查找ErrorCode
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return SYSTEM_ERROR;
    }
}

