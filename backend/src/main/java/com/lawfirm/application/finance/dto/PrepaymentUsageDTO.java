package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 预收款核销记录DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepaymentUsageDTO extends BaseDTO {

  /** 预收款ID */
  private Long prepaymentId;

  /** 预收款编号 */
  private String prepaymentNo;

  /** 收费记录ID */
  private Long feeId;

  /** 收费编号 */
  private String feeNo;

  /** 收费名称 */
  private String feeName;

  /** 案件ID */
  private Long matterId;

  /** 案件编号 */
  private String matterNo;

  /** 案件名称 */
  private String matterName;

  /** 核销金额 */
  private BigDecimal amount;

  /** 核销时间 */
  private LocalDateTime usageTime;

  /** 操作人ID */
  private Long operatorId;

  /** 操作人名称 */
  private String operatorName;

  /** 备注 */
  private String remark;
}
