package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 利冲检查查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConflictCheckQueryDTO extends PageQuery {

  /** 检查编号 */
  private String checkNo;

  /** 案件ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 状态 */
  private String status;

  /** 结果 */
  private String result;

  /** 申请人ID */
  private Long applicantId;
}
