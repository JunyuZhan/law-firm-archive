package com.lawfirm.application.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 提成记录 DTO */
@Data
public class CommissionDTO {

  /** 提成ID */
  private Long id;

  /** 收款ID */
  private Long paymentId;

  /** 案件ID */
  private Long matterId;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 规则ID */
  private Long ruleId;

  /** 总收入 */
  private BigDecimal grossAmount;

  /** 税额 */
  private BigDecimal taxAmount;

  /** 管理费 */
  private BigDecimal managementFee;

  /** 成本金额 */
  private BigDecimal costAmount;

  /** 净收入 */
  private BigDecimal netAmount;

  /** 分配比例 */
  private BigDecimal distributionRatio;

  /** 提成比例 */
  private BigDecimal commissionRate;

  /** 提成金额 */
  private BigDecimal commissionAmount;

  /** 薪酬类型 */
  private String compensationType;

  /** 状态 */
  private String status;

  /** 备注 */
  private String remark;

  /** 审批人ID */
  private Long approvedBy;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 支付时间 */
  private LocalDateTime paidAt;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
