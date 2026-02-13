package com.lawfirm.application.ai.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** AI月度账单DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class AiMonthlyBillDTO extends BaseDTO {

  /** 账单年份 */
  private Integer billYear;

  /** 账单月份（1-12） */
  private Integer billMonth;

  /** 账单月份显示（yyyy-MM） */
  private String billMonthDisplay;

  /** 用户ID */
  private Long userId;

  /** 用户姓名 */
  private String userName;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

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

  /** 当月收费比例（%） */
  private Integer chargeRatio;

  /** 扣减状态：PENDING-待扣减，DEDUCTED-已扣减，WAIVED-已减免 */
  private String deductionStatus;

  /** 扣减状态名称 */
  private String deductionStatusName;

  /** 实际扣减金额（元） */
  private BigDecimal deductionAmount;

  /** 扣减时间 */
  private LocalDateTime deductedAt;

  /** 操作人ID */
  private Long deductedBy;

  /** 操作人姓名 */
  private String deductedByName;

  /** 扣减备注 */
  private String deductionRemark;

  /** 关联的工资扣减记录ID */
  private Long salaryDeductionId;
}
