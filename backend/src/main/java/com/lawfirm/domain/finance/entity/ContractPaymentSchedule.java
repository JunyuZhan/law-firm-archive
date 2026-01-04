package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同付款计划实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("contract_payment_schedule")
public class ContractPaymentSchedule extends BaseEntity {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 阶段名称（如：签约款、一审结束、执行到位）
     */
    private String phaseName;

    /**
     * 付款金额
     */
    private BigDecimal amount;

    /**
     * 比例（风险代理时使用，百分比）
     */
    private BigDecimal percentage;

    /**
     * 计划收款日期
     */
    private LocalDate plannedDate;

    /**
     * 实际收款日期
     */
    private LocalDate actualDate;

    /**
     * 状态：PENDING-待收, PARTIAL-部分收款, PAID-已收清, CANCELLED-已取消
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
