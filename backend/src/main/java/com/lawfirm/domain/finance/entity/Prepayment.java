package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预收款实体
 * 
 * 预收款是指客户在项目开始前或服务完成前预先支付的款项，
 * 需要在后续服务完成后进行核销。
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_prepayment")
public class Prepayment extends BaseEntity {

    /**
     * 预收款编号
     */
    private String prepaymentNo;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 合同ID（可选，预收款可能在签订合同前收取）
     */
    private Long contractId;

    /**
     * 项目ID（可选，预收款可能在立项前收取）
     */
    private Long matterId;

    /**
     * 预收款金额
     */
    private BigDecimal amount;

    /**
     * 已核销金额
     */
    private BigDecimal usedAmount;

    /**
     * 剩余金额
     */
    private BigDecimal remainingAmount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 收款日期
     */
    private LocalDate receiptDate;

    /**
     * 收款方式：BANK-银行转账, CASH-现金, CHECK-支票, OTHER-其他
     */
    private String paymentMethod;

    /**
     * 收款账户
     */
    private String bankAccount;

    /**
     * 交易流水号
     */
    private String transactionNo;

    /**
     * 状态：PENDING-待确认, ACTIVE-有效, USED-已用完, REFUNDED-已退款, CANCELLED-已取消
     */
    private String status;

    /**
     * 确认人ID
     */
    private Long confirmerId;

    /**
     * 确认时间
     */
    private LocalDateTime confirmedAt;

    /**
     * 预收款用途说明
     */
    private String purpose;

    /**
     * 备注
     */
    private String remark;
}
