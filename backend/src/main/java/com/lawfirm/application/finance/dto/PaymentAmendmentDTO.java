package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收款变更申请DTO
 *
 * <p>Requirements: 3.5
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentAmendmentDTO extends BaseDTO {

  /** 关联收款记录ID */
  private Long paymentId;

  /** 收款编号 */
  private String paymentNo;

  /** 原金额 */
  private BigDecimal originalAmount;

  /** 新金额 */
  private BigDecimal newAmount;

  /** 金额差异 */
  private BigDecimal amountDiff;

  /** 变更原因 */
  private String reason;

  /** 申请人ID */
  private Long requestedBy;

  /** 申请人姓名 */
  private String requestedByName;

  /** 申请时间 */
  private LocalDateTime requestedAt;

  /** 审批人ID */
  private Long approvedBy;

  /** 审批人姓名 */
  private String approvedByName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 拒绝原因 */
  private String rejectReason;
}
