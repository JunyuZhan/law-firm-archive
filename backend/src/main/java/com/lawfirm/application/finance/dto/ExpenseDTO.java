package com.lawfirm.application.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 费用报销 DTO */
@Data
public class ExpenseDTO {

  /** 费用ID */
  private Long id;

  /** 费用编号 */
  private String expenseNo;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 申请人ID */
  private Long applicantId;

  /** 申请人名称 */
  private String applicantName;

  /** 费用类型 */
  private String expenseType;

  /** 费用类别 */
  private String expenseCategory;

  /** 费用日期 */
  private LocalDate expenseDate;

  /** 费用金额 */
  private BigDecimal amount;

  /** 币种 */
  private String currency;

  /** 费用描述 */
  private String description;

  /** 供应商名称 */
  private String vendorName;

  /** 发票号 */
  private String invoiceNo;

  /** 发票URL */
  private String invoiceUrl;

  /** 状态 */
  private String status;

  /** 审批人ID */
  private Long approverId;

  /** 审批人名称 */
  private String approverName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 审批意见 */
  private String approvalComment;

  /** 支付时间 */
  private LocalDateTime paidAt;

  /** 支付人ID */
  private Long paidBy;

  /** 支付人名称 */
  private String paidByName;

  /** 支付方式 */
  private String paymentMethod;

  /** 是否成本分摊 */
  private Boolean isCostAllocation;

  /** 分摊到案件ID */
  private Long allocatedToMatterId;

  /** 备注 */
  private String remark;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
