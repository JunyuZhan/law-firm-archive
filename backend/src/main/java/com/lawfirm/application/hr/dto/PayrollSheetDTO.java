package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工资表 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollSheetDTO extends BaseDTO {

  /** 工资表编号 */
  private String payrollNo;

  /** 工资年份 */
  private Integer payrollYear;

  /** 工资月份 */
  private Integer payrollMonth;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 员工总数 */
  private Integer totalEmployees;

  /** 应发总额 */
  private BigDecimal totalGrossAmount;

  /** 扣除总额 */
  private BigDecimal totalDeductionAmount;

  /** 实发总额 */
  private BigDecimal totalNetAmount;

  /** 已确认数量 */
  private Integer confirmedCount;

  /** 提交时间 */
  private LocalDateTime submittedAt;

  /** 提交人ID */
  private Long submittedBy;

  /** 提交人名称 */
  private String submittedByName;

  /** 财务确认时间 */
  private LocalDateTime financeConfirmedAt;

  /** 财务确认人ID */
  private Long financeConfirmedBy;

  /** 财务确认人名称 */
  private String financeConfirmedByName;

  /** 发放时间 */
  private LocalDateTime issuedAt;

  /** 发放人ID */
  private Long issuedBy;

  /** 发放人名称 */
  private String issuedByName;

  /** 支付方式 */
  private String paymentMethod;

  /** 支付方式名称 */
  private String paymentMethodName;

  /** 支付凭证URL */
  private String paymentVoucherUrl;

  /** 备注 */
  private String remark;

  /** 自动确认截止时间 */
  private LocalDateTime autoConfirmDeadline;

  /** 审批人ID */
  private Long approverId;

  /** 审批人名称 */
  private String approverName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 审批人ID */
  private Long approvedBy;

  /** 审批人名称 */
  private String approvedByName;

  /** 审批意见 */
  private String approvalComment;

  /** 工资明细列表（详情时使用） */
  private List<PayrollItemDTO> items;

  /** 未确认的员工列表（包含拒绝理由） */
  private List<PayrollItemDTO> rejectedItems;
}
