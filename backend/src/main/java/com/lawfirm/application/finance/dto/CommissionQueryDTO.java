package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 提成查询 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommissionQueryDTO extends PageQuery {

  /** 收款ID */
  private Long paymentId;

  /** 案件ID */
  private Long matterId;

  /** 用户ID */
  private Long userId;

  /** 状态 */
  private String status;
}
