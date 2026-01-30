package com.lawfirm.domain.ai.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI使用记录实体 记录每次AI调用的详细信息 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageLog {

  /** 主键ID */
  private Long id;

  // 用户信息
  /** 用户ID */
  private Long userId;

  /** 用户名 */
  private String userName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  // AI模型信息
  /** 集成ID */
  private Long integrationId;

  /** 集成编码 */
  private String integrationCode;

  /** 集成名称 */
  private String integrationName;

  /** 模型名称 */
  private String modelName;

  // 调用信息
  /** 请求类型：DOCUMENT_GENERATE/CHAT/SUMMARY/ANALYSIS */
  private String requestType;

  /** 业务类型：MATTER/PERSONAL */
  private String businessType;

  /** 业务ID */
  private Long businessId;

  // Token统计
  /** 提示Token数 */
  private Integer promptTokens;

  /** 完成Token数 */
  private Integer completionTokens;

  /** 总Token数 */
  private Integer totalTokens;

  // 费用信息（单价：元/千Token，费用：元）
  /** 提示单价（元/千Token） */
  private BigDecimal promptPrice;

  /** 完成单价（元/千Token） */
  private BigDecimal completionPrice;

  /** 总费用（元） */
  private BigDecimal totalCost;

  /** 用户费用（元） */
  private BigDecimal userCost;

  /** 收费比例快照（百分比） */
  private Integer chargeRatio;

  // 调用结果
  /** 是否成功 */
  private Boolean success;

  /** 错误信息 */
  private String errorMessage;

  /** 调用时长（毫秒） */
  private Integer durationMs;

  // 元数据
  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 请求类型枚举 */
  public static class RequestType {
    /** 文档生成 */
    public static final String DOCUMENT_GENERATE = "DOCUMENT_GENERATE";

    /** 聊天 */
    public static final String CHAT = "CHAT";

    /** 摘要 */
    public static final String SUMMARY = "SUMMARY";

    /** 分析 */
    public static final String ANALYSIS = "ANALYSIS";

    /** 通用 */
    public static final String GENERAL = "GENERAL";
  }

  /** 业务类型枚举 */
  public static class BusinessType {
    /** 案件相关 */
    public static final String MATTER = "MATTER";

    /** 个人相关 */
    public static final String PERSONAL = "PERSONAL";
  }
}
