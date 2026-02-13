package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 收费记录查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeQueryDTO extends PageQuery {

  /** 收费编号 */
  private String feeNo;

  /** 合同ID */
  private Long contractId;

  /** 案件ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 收费类型 */
  private String feeType;

  /** 状态 */
  private String status;

  /** 计划日期起始 */
  private LocalDate plannedDateFrom;

  /** 计划日期结束 */
  private LocalDate plannedDateTo;

  /** 负责人ID */
  private Long responsibleId;
}
