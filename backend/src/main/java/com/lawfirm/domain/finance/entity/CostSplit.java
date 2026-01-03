package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 成本分摊实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_cost_split")
public class CostSplit extends BaseEntity {

    /**
     * 费用ID（公共费用）
     */
    private Long expenseId;

    /**
     * 分摊到的项目ID
     */
    private Long matterId;

    /**
     * 分摊金额
     */
    private BigDecimal splitAmount;

    /**
     * 分摊比例（可选，如0.25表示25%）
     */
    private BigDecimal splitRatio;

    /**
     * 分摊方式：EQUAL-平均分摊, RATIO-按比例, MANUAL-手动指定
     */
    private String splitMethod;

    /**
     * 分摊日期
     */
    private LocalDate splitDate;

    /**
     * 分摊操作人ID
     */
    private Long splitBy;

    /**
     * 备注
     */
    private String remark;
}

