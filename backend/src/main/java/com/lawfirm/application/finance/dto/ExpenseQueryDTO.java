package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 费用报销查询 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpenseQueryDTO extends PageQuery {

  /** 费用编号 */
  private String expenseNo;

  /** 案件ID */
  private Long matterId;

  /** 申请人ID */
  private Long applicantId;

  /** 状态 */
  private String status;

  /** 费用类型 */
  private String expenseType;

  /** 费用分类 */
  private String expenseCategory;
}
