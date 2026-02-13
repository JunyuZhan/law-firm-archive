package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/** 外部系统集成配置DTO. */
@Data
public class ExternalIntegrationDTO {

  /** 集成配置ID */
  private Long id;

  /** 集成编码. */
  private String integrationCode;

  /** 集成名称. */
  private String integrationName;

  /** 集成类型. */
  private String integrationType;

  /** 描述. */
  private String description;

  /** API地址. */
  private String apiUrl;

  /** API密钥（脱敏显示）. */
  private String apiKey;

  /** API密钥是否已配置. */
  private Boolean hasApiSecret;

  /** 认证方式. */
  private String authType;

  /** 额外配置. */
  private Map<String, Object> extraConfig;

  /** 是否启用. */
  private Boolean enabled;

  /** 最后测试时间. */
  private LocalDateTime lastTestTime;

  /** 最后测试结果. */
  private String lastTestResult;

  /** 最后测试消息. */
  private String lastTestMessage;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
