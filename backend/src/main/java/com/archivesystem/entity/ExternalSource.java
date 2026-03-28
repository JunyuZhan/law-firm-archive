package com.archivesystem.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 外部系统来源配置实体类.
 * 对应数据库表: arc_external_source
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "arc_external_source", autoResultMap = true)
public class ExternalSource extends BaseEntity {

    /** 来源编码 */
    @NotBlank(message = "来源编码不能为空")
    @Size(max = 50, message = "来源编码长度不能超过50个字符")
    private String sourceCode;
    
    /** 来源名称 */
    @NotBlank(message = "来源名称不能为空")
    @Size(max = 100, message = "来源名称长度不能超过100个字符")
    private String sourceName;
    
    /** 来源类型 */
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;
    
    /** 说明 */
    @Size(max = 500, message = "说明长度不能超过500个字符")
    private String description;

    // ===== 接口配置 =====
    
    /** API地址 */
    private String apiUrl;
    
    /** API密钥 */
    private String apiKey;
    
    /** 认证方式 */
    private String authType;
    
    /** 扩展配置 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extraConfig;

    // ===== 状态信息 =====
    
    /** 是否启用 */
    @Builder.Default
    private Boolean enabled = false;
    
    /** 最后同步时间 */
    private LocalDateTime lastSyncAt;
    
    /** 最后同步状态 */
    private String lastSyncStatus;
    
    /** 最后同步消息 */
    private String lastSyncMessage;

    public static final String TYPE_LAW_FIRM = "LAW_FIRM";
    public static final String TYPE_COURT = "COURT";
    public static final String TYPE_ENTERPRISE = "ENTERPRISE";
    public static final String TYPE_OTHER = "OTHER";
    
    public static final String AUTH_API_KEY = "API_KEY";
    public static final String AUTH_OAUTH2 = "OAUTH2";
    public static final String AUTH_BASIC = "BASIC";
}
