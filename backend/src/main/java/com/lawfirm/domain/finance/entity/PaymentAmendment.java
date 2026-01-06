package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款变更申请实体
 * 用于已锁定收款记录的变更申请和审批流程
 * 
 * Requirements: 3.5
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("fin_payment_amendment")
public class PaymentAmendment extends BaseEntity {

    /**
     * 关联收款记录ID
     */
    private Long paymentId;

    /**
     * 原金额
     */
    private BigDecimal originalAmount;

    /**
     * 新金额
     */
    private BigDecimal newAmount;

    /**
     * 变更原因
     */
    private String reason;

    /**
     * 申请人ID
     */
    private Long requestedBy;

    /**
     * 申请时间
     */
    private LocalDateTime requestedAt;

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 状态：PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝
     */
    private String status;

    /**
     * 拒绝原因
     */
    private String rejectReason;
}
