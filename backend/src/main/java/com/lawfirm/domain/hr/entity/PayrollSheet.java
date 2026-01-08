package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工资表实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_payroll_sheet")
public class PayrollSheet extends BaseEntity {

    /**
     * 工资表编号（格式：PAY-YYYYMM）
     */
    private String payrollNo;

    /**
     * 工资年份
     */
    private Integer payrollYear;

    /**
     * 工资月份（1-12）
     */
    private Integer payrollMonth;

    /**
     * 状态：DRAFT-草稿, PENDING_CONFIRM-待确认, CONFIRMED-已确认, PENDING_APPROVAL-待审批, APPROVED-已审批, REJECTED-已拒绝, ISSUED-已发放
     */
    private String status;

    /**
     * 总人数
     */
    private Integer totalEmployees;

    /**
     * 应发工资总额
     */
    private BigDecimal totalGrossAmount;

    /**
     * 扣减总额
     */
    private BigDecimal totalDeductionAmount;

    /**
     * 实发工资总额
     */
    private BigDecimal totalNetAmount;

    /**
     * 已确认人数
     */
    private Integer confirmedCount;

    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;

    /**
     * 提交人ID
     */
    private Long submittedBy;

    /**
     * 财务确认时间
     */
    private LocalDateTime financeConfirmedAt;

    /**
     * 财务确认人ID
     */
    private Long financeConfirmedBy;

    /**
     * 发放时间
     */
    private LocalDateTime issuedAt;

    /**
     * 发放人ID
     */
    private Long issuedBy;

    /**
     * 发放方式：BANK_TRANSFER-银行转账, CASH-现金, OTHER-其他
     */
    private String paymentMethod;

    /**
     * 发放凭证URL
     */
    private String paymentVoucherUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 自动确认截止时间（超过此时间未确认的工资明细将自动确认）
     */
    private LocalDateTime autoConfirmDeadline;

    /**
     * 审批人ID（主任或合伙人）
     */
    private Long approverId;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批意见
     */
    private String approvalComment;
}

