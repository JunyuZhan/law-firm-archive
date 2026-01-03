package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本归集记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_cost_allocation")
public class CostAllocation extends BaseEntity {

    /**
     * 项目ID
     */
    private Long matterId;

    /**
     * 费用ID
     */
    private Long expenseId;

    /**
     * 归集金额
     */
    private BigDecimal allocatedAmount;

    /**
     * 归集日期
     */
    private LocalDate allocationDate;

    /**
     * 归集操作人ID
     */
    private Long allocatedBy;

    /**
     * 备注
     */
    private String remark;
}

