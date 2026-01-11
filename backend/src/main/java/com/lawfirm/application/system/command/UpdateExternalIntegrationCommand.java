package com.lawfirm.application.system.command;

import lombok.Data;

import java.util.Map;

/**
 * 创建/更新外部系统集成配置命令
 */
@Data
public class UpdateExternalIntegrationCommand {
    
    /** ID（更新时必填，创建时为空） */
    private Long id;

    /** 集成编码（创建时必填） */
    private String integrationCode;

    /** 集成名称（创建时必填） */
    private String integrationName;

    /** 集成类型：ARCHIVE/AI/CLIENT_SERVICE/OTHER（创建时必填） */
    private String integrationType;

    /** API地址 */
    private String apiUrl;

    /** API密钥 */
    private String apiKey;

    /** API密钥（加密存储） */
    private String apiSecret;

    /** 认证方式 */
    private String authType;

    /** 额外配置 */
    private Map<String, Object> extraConfig;

    /** 描述 */
    private String description;
}

