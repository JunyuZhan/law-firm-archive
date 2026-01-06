package com.lawfirm.domain.finance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 提成规则实体
 * 定义案件收入在律所、主办律师、协办律师、辅助人员之间的分配比例
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
     * 规则类型（保留字段）
     */
    private String ruleType;

    /**
     * 律所比例（%）
     */
    private BigDecimal firmRate;

    /**
     * 主办律师比例（%）
     */
    private BigDecimal leadLawyerRate;

    /**
     * 协办律师比例（%）
     */
    private BigDecimal assistLawyerRate;

    /**
     * 辅助人员比例（%）
     */
    private BigDecimal supportStaffRate;

    /**
     * 案源人比例（%）
     */
    private BigDecimal originatorRate;

    /**
     * 律师创建合同时是否允许修改比例
     */
    private Boolean allowModify;

    /**
     * 方案说明
     */
    private String description;

    /**
     * 是否默认规则
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean active;
}
