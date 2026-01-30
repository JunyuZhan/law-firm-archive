package com.lawfirm.application.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 成本归集 DTO */
@Data
public class CostAllocationDTO {

  /** 归集ID */
  private Long id;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

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

  /** 归集金额 */
  private BigDecimal allocatedAmount;

  /** 归集日期 */
  private LocalDate allocationDate;

  /** 归集人ID */
  private Long allocatedBy;

  /** 归集人名称 */
  private String allocatedByName;

  /** 备注 */
  private String remark;

  /** 创建日期 */
  private LocalDate createdAt;
}
