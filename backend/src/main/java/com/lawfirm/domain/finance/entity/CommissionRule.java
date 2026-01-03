package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 提成规则实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("finance_commission_rule")
public class CommissionRule extends BaseEntity {

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 律所留存比例
     */
    private BigDecimal firmRetentionRate;

    /**
     * 案源提成比例
     */
    private BigDecimal originatorRate;

    /**
     * 税费率
     */
    private BigDecimal taxRate;

    /**
     * 管理费率
     */
    private BigDecimal managementFeeRate;

    /**
     * 阶梯费率（JSON格式）
     */
    private String rateTiers;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expiryDate;

    /**
     * 是否默认规则
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean active;
}

