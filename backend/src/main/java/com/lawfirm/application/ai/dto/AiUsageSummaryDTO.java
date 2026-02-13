package com.lawfirm.application.ai.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** AI使用统计DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageSummaryDTO {

  /** 用户ID */
  private Long userId;

  /** 用户姓名 */
  private String userName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 统计月份（格式：yyyy-MM） */
  private String month;

  /** 调用次数 */
  private Integer totalCalls;

  /** 总Token数 */
  private Long totalTokens;

  /** 输入Token数 */
  private Long promptTokens;

  /** 输出Token数 */
  private Long completionTokens;

  /** API总费用（元） */
  private BigDecimal totalCost;

  /** 用户应付费用（元） */
  private BigDecimal userCost;

  /** 单位承担费用（元） */
  private BigDecimal companyCost;

  /** 收费比例（%） */
  private Integer chargeRatio;

  /** 月度Token配额 */
  private Long monthlyTokenQuota;

  /** 月度费用配额（元） */
  private BigDecimal monthlyCostQuota;

  /** Token使用百分比 */
  private Double tokenUsagePercent;

  /** 费用使用百分比 */
  private Double costUsagePercent;

  /** 平均每次调用Token数 */
  private Double avgTokensPerCall;

  /** 平均每次调用费用（元） */
  private BigDecimal avgCostPerCall;
}
