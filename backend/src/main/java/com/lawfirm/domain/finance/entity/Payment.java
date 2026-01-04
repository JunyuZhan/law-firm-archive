package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收款记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_payment")
public class Payment extends BaseEntity {

    /**
     * 收款编号
     */
    private String paymentNo;

    /**
     * 关联收费ID
     */
    private Long feeId;

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 客户ID
     */
    private Long clientId;

    /**
     * 收款金额
     */
    private BigDecimal amount;

    /**
     * 币种
     */
    private String currency;

    /**
     * 收款方式：BANK-银行转账, CASH-现金, CHECK-支票, OTHER-其他
     */
    private String paymentMethod;

    /**
     * 收款日期
     */
    private LocalDate paymentDate;

    /**
     * 收款账户
     */
    @TableField("payment_account")
    private String bankAccount;

    /**
     * 交易流水号
     */
    @TableField("receipt_no")
    private String transactionNo;

    /**
     * 状态：PENDING-待确认, CONFIRMED-已确认, CANCELLED-已取消
     */
    private String status;

    /**
     * 确认人ID
     */
    @TableField("confirmed_by")
    private Long confirmerId;

    /**
     * 备注
     */
    private String remark;
}

