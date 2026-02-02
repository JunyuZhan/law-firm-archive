package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import com.lawfirm.infrastructure.persistence.typehandler.PostgresJsonTypeHandler;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 外部系统集成配置实体 */
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
  /** 类型：档案系统 */
  public static final String TYPE_ARCHIVE = "ARCHIVE";

  /** 类型：AI服务 */
  public static final String TYPE_AI = "AI";

  /** 类型：OCR服务 */
  public static final String TYPE_OCR = "OCR";

  /** 类型：存储服务 */
  public static final String TYPE_STORAGE = "STORAGE";

  /** 类型：通知服务 */
  public static final String TYPE_NOTIFICATION = "NOTIFICATION";

  /** 类型：客户服务系统 */
  public static final String TYPE_CLIENT_SERVICE = "CLIENT_SERVICE";

  /** 类型：企业信息 */
  public static final String TYPE_ENTERPRISE_INFO = "ENTERPRISE_INFO";

  /** 类型：其他 */
  public static final String TYPE_OTHER = "OTHER";

  // ===== 认证方式常量 =====
  /** 认证方式：API密钥 */
  public static final String AUTH_API_KEY = "API_KEY";

  /** 认证方式：Bearer Token */
  public static final String AUTH_BEARER_TOKEN = "BEARER_TOKEN";

  /** 认证方式：Basic认证 */
  public static final String AUTH_BASIC = "BASIC";

  /** 认证方式：OAuth2 */
  public static final String AUTH_OAUTH2 = "OAUTH2";

  /** 认证方式：Webhook */
  public static final String AUTH_WEBHOOK = "WEBHOOK";

  // ===== 测试结果常量 =====
  /** 测试结果：成功 */
  public static final String TEST_SUCCESS = "SUCCESS";

  /** 测试结果：失败 */
  public static final String TEST_FAILED = "FAILED";
}
