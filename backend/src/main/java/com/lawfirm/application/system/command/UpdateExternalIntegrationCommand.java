package com.lawfirm.application.system.command;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * 更新外部系统集成配置命令
 */
@Data
public class UpdateExternalIntegrationCommand {
    
    @NotNull(message = "ID不能为空")
    private Long id;

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

