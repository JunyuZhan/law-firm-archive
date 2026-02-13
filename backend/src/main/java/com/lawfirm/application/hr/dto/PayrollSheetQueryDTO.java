package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工资表查询 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollSheetQueryDTO extends PageQuery {

  /** 工资年份. */
  private Integer payrollYear;

  /** 工资月份. */
  private Integer payrollMonth;

  /** 状态. */
  private String status;

  /** 工资表编号. */
  private String payrollNo;
}
