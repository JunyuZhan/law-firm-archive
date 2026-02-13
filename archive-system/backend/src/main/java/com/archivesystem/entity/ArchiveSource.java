package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 档案来源配置实体类.
 * 用于配置可接收档案的外部系统。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "archive_source", autoResultMap = true)
public class ArchiveSource extends BaseEntity {

    /** 来源编码 */
    private String sourceCode;

    /** 来源名称 */
    private String sourceName;

    /** 来源类型：LAW_FIRM-律所系统, COURT-法院系统, ENTERPRISE-企业系统, OTHER-其他 */
    private String sourceType;

    /** 描述 */
    private String description;

    /** API地址 */
    private String apiUrl;

    /** API密钥 */
    private String apiKey;

    /** API密钥（加密存储） */
    private String apiSecret;

    /** 认证方式：API_KEY, BEARER_TOKEN, BASIC */
    private String authType;

    /** 额外配置（JSON格式） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraConfig;

    /** 是否启用 */
    private Boolean enabled;

    /** 最后同步时间 */
    private LocalDateTime lastSyncAt;

    /** 最后同步结果 */
    private String lastSyncResult;

    /** 最后同步消息 */
    private String lastSyncMessage;

    // ===== 来源类型常量 =====
    
    public static final String TYPE_LAW_FIRM = "LAW_FIRM";
    public static final String TYPE_COURT = "COURT";
    public static final String TYPE_ENTERPRISE = "ENTERPRISE";
    public static final String TYPE_OTHER = "OTHER";

    // ===== 认证方式常量 =====
    
    public static final String AUTH_API_KEY = "API_KEY";
    public static final String AUTH_BEARER_TOKEN = "BEARER_TOKEN";
    public static final String AUTH_BASIC = "BASIC";
}
