package com.lawfirm.application.ai.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI使用记录DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiUsageLogDTO extends BaseDTO {

  /** 用户ID */
  private Long userId;

  /** 用户姓名 */
  private String userName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** AI集成配置ID */
  private Long integrationId;

  /** 集成编码（如AI_DEEPSEEK） */
  private String integrationCode;

  /** 集成名称 */
  private String integrationName;

  /** 具体模型名（如deepseek-chat） */
  private String modelName;

  /** 请求类型：DOCUMENT_GENERATE/CHAT/SUMMARY等 */
  private String requestType;

  /** 请求类型名称 */
  private String requestTypeName;

  /** 业务类型：MATTER/PERSONAL */
  private String businessType;

  /** 业务类型名称 */
  private String businessTypeName;

  /** 业务ID（如项目ID） */
  private Long businessId;

  /** 业务名称（如项目名称，冗余便于展示） */
  private String businessName;

  /** 输入Token数 */
  private Integer promptTokens;

  /** 输出Token数 */
  private Integer completionTokens;

  /** 总Token数 */
  private Integer totalTokens;

  /** 输入Token单价（元/千Token） */
  private BigDecimal promptPrice;

  /** 输出Token单价（元/千Token） */
  private BigDecimal completionPrice;

  /** 总费用（元） */
  private BigDecimal totalCost;

  /** 用户承担费用（元） */
  private BigDecimal userCost;

  /** 单位承担费用（元） */
  private BigDecimal companyCost;

  /** 当时的收费比例（记录快照） */
  private Integer chargeRatio;

  /** 是否成功 */
  private Boolean success;

  /** 错误信息 */
  private String errorMessage;

  /** 响应时间（毫秒） */
  private Integer durationMs;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
