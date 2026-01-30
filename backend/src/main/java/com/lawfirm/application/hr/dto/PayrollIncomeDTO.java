package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工资收入项 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollIncomeDTO extends BaseDTO {

  /** 工资明细ID */
  private Long payrollItemId;

  /** 收入类型 */
  private String incomeType;

  /** 收入类型名称 */
  private String incomeTypeName;

  /** 金额 */
  private BigDecimal amount;

  /** 备注 */
  private String remark;

  /** 来源类型 */
  private String sourceType;

  /** 来源类型名称 */
  private String sourceTypeName;

  /** 来源ID */
  private Long sourceId;
}
