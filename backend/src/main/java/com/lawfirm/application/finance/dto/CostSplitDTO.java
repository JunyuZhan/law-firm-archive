package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 成本分摊 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class CostSplitDTO extends BaseDTO {

  /** 费用ID */
  private Long expenseId;

  /** 费用编号 */
  private String expenseNo;

  /** 费用描述 */
  private String expenseDescription;

  /** 费用类型 */
  private String expenseType;

  /** 费用日期 */
  private LocalDate expenseDate;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 分摊金额 */
  private BigDecimal splitAmount;

  /** 分摊比例 */
  private BigDecimal splitRatio;

  /** 分摊方式 */
  private String splitMethod;

  /** 分摊日期 */
  private LocalDate splitDate;

  /** 操作人ID */
  private Long splitBy;

  /** 操作人姓名 */
  private String splitByName;

  /** 备注 */
  private String remark;
}
