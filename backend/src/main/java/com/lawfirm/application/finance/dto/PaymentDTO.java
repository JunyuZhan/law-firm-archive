package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 收款记录 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentDTO extends BaseDTO {

  /** 收款编号 */
  private String paymentNo;

  /** 收费记录ID */
  private Long feeId;

  /** 合同ID */
  private Long contractId;

  /** 案件ID */
  private Long matterId;

  /** 案件名称 */
  private String matterName;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 收款金额 */
  private BigDecimal amount;

  /** 币种 */
  private String currency;

  /** 收款方式 */
  private String paymentMethod;

  /** 收款方式名称 */
  private String paymentMethodName;

  /** 收款日期 */
  private LocalDate paymentDate;

  /** 银行账户 */
  private String bankAccount;

  /** 交易流水号 */
  private String transactionNo;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 确认人ID */
  private Long confirmerId;

  /** 确认人名称 */
  private String confirmerName;

  /** 备注 */
  private String remark;
}
