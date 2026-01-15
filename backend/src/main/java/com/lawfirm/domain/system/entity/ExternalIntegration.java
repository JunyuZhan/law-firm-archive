package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import com.lawfirm.infrastructure.persistence.typehandler.PostgresJsonTypeHandler;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 外部系统集成配置实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_external_integration", autoResultMap = true)
public class ExternalIntegration extends BaseEntity {

    /** 集成编码 */
    private String integrationCode;

    /** 集成名称 */
    private String integrationName;

    /** 集成类型 */
    private String integrationType;

    /** 描述 */
    private String description;

    /** API地址 */
    private String apiUrl;

    /** API密钥 */
    private String apiKey;

    /** API密钥（加密存储） */
    private String apiSecret;

    /** 认证方式 */
    private String authType;

    /** 额外配置（JSON格式） */
    @TableField(typeHandler = PostgresJsonTypeHandler.class)
    private Map<String, Object> extraConfig;

    /** 是否启用 */
    private Boolean enabled;

    /** 最后测试时间 */
    private LocalDateTime lastTestTime;

    /** 最后测试结果 */
    private String lastTestResult;

    /** 最后测试消息 */
    private String lastTestMessage;

    // ===== 集成类型常量 =====
    public static final String TYPE_ARCHIVE = "ARCHIVE";
    public static final String TYPE_AI = "AI";
    public static final String TYPE_OCR = "OCR";
    public static final String TYPE_STORAGE = "STORAGE";
    public static final String TYPE_NOTIFICATION = "NOTIFICATION";
    public static final String TYPE_ENTERPRISE_INFO = "ENTERPRISE_INFO";
    public static final String TYPE_OTHER = "OTHER";

    // ===== 认证方式常量 =====
    public static final String AUTH_API_KEY = "API_KEY";
    public static final String AUTH_BEARER_TOKEN = "BEARER_TOKEN";
    public static final String AUTH_BASIC = "BASIC";
    public static final String AUTH_OAUTH2 = "OAUTH2";
    public static final String AUTH_WEBHOOK = "WEBHOOK";

    // ===== 测试结果常量 =====
    public static final String TEST_SUCCESS = "SUCCESS";
    public static final String TEST_FAILED = "FAILED";
}

