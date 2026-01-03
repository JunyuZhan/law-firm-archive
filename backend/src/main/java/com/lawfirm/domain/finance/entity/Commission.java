package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 提成记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_commission")
public class Commission extends BaseEntity {

    /**
     * 收款ID
     */
    private Long paymentId;

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 毛收入
     */
    private BigDecimal grossAmount;

    /**
     * 税费
     */
    private BigDecimal taxAmount;

    /**
     * 管理费
     */
    private BigDecimal managementFee;

    /**
     * 成本
     */
    private BigDecimal costAmount;

    /**
     * 净收入
     */
    private BigDecimal netAmount;

    /**
     * 分配比例
     */
    private BigDecimal distributionRatio;

    /**
     * 提成比例
     */
    private BigDecimal commissionRate;

    /**
     * 提成金额
     */
    private BigDecimal commissionAmount;

    /**
     * 薪酬模式（记录计算时的模式）
     */
    private String compensationType;

    /**
     * 状态：CALCULATED-已计算, APPROVED-已审批, PAID-已发放, SALARIED_EXEMPT-授薪豁免
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 审批人ID
     */
    private Long approvedBy;

    /**
     * 审批时间
     */
    private LocalDateTime approvedAt;

    /**
     * 发放时间
     */
    private LocalDateTime paidAt;
}

