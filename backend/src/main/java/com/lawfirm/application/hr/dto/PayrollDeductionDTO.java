package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工资扣减项 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollDeductionDTO extends BaseDTO {

  /** 工资明细ID */
  private Long payrollItemId;

  /** 扣减类型 */
  private String deductionType;

  /** 扣减类型名称 */
  private String deductionTypeName;

  /** 金额 */
  private BigDecimal amount;

  /** 备注 */
  private String remark;

  /** 来源类型 */
  private String sourceType;

  /** 来源类型名称 */
  private String sourceTypeName;
}
