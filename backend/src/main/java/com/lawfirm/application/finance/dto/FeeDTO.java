package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 收费记录 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class FeeDTO extends BaseDTO {

  /** 收费编号 */
  private String feeNo;

  /** 合同ID */
  private Long contractId;

  /** 合同名称 */
  private String contractName;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 收费类型 */
  private String feeType;

  /** 收费类型名称 */
  private String feeTypeName;

  /** 收费名称 */
  private String feeName;

  /** 收费金额 */
  private BigDecimal amount;

  /** 已收金额 */
  private BigDecimal paidAmount;

  /** 未收金额 */
  private BigDecimal unpaidAmount;

  /** 币种 */
  private String currency;

  /** 计划日期 */
  private LocalDate plannedDate;

  /** 实际日期 */
  private LocalDate actualDate;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 负责人ID */
  private Long responsibleId;

  /** 负责人名称 */
  private String responsibleName;

  /** 备注 */
  private String remark;

  /** 收款记录 */
  private List<PaymentDTO> payments;
}
