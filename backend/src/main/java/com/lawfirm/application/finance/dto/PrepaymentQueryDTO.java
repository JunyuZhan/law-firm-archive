package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 预收款查询DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepaymentQueryDTO extends PageQuery {

  /** 预收款编号 */
  private String prepaymentNo;

  /** 客户ID */
  private Long clientId;

  /** 合同ID */
  private Long contractId;

  /** 案件ID */
  private Long matterId;

  /** 状态 */
  private String status;
}
