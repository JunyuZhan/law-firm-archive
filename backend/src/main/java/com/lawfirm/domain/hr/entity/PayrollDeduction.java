package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 工资扣减项实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_payroll_deduction")
public class PayrollDeduction extends BaseEntity {

  /** 工资明细ID */
  private Long payrollItemId;

  /** 扣减类型：INCOME_TAX-个人所得税, SOCIAL_INSURANCE-社保个人部分, HOUSING_FUND-公积金个人部分, OTHER_DEDUCTION-其他扣款 */
  private String deductionType;

  /** 金额 */
  private BigDecimal amount;

  /** 备注 */
  private String remark;

  /** 数据来源：AUTO-自动计算, MANUAL-手动输入, IMPORT-导入 */
  private String sourceType;
}
