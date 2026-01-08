package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * 工资收入项实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_payroll_income")
public class PayrollIncome extends BaseEntity {

    /**
     * 工资明细ID
     */
    private Long payrollItemId;

    /**
     * 收入类型：BASE_SALARY-基本工资, COMMISSION-提成, PERFORMANCE_BONUS-绩效奖金, OTHER_ALLOWANCE-其他津贴
     */
    private String incomeType;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 数据来源：AUTO-自动汇总, MANUAL-手动输入, IMPORT-导入
     */
    private String sourceType;

    /**
     * 来源ID（如提成记录ID、合同ID等）
     */
    private Long sourceId;
}

