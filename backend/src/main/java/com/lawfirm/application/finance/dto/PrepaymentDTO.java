package com.lawfirm.application.finance.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 预收款DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class PrepaymentDTO extends BaseDTO {

  /** 预收款编号 */
  private String prepaymentNo;

  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 合同ID */
  private Long contractId;

  /** 合同编号 */
  private String contractNo;

  /** 案件ID */
  private Long matterId;

  /** 案件编号 */
  private String matterNo;

  /** 案件名称 */
  private String matterName;

  /** 预收款金额 */
  private BigDecimal amount;

  /** 已使用金额 */
  private BigDecimal usedAmount;

  /** 剩余金额 */
  private BigDecimal remainingAmount;

  /** 币种 */
  private String currency;

  /** 收款日期 */
  private LocalDate receiptDate;

  /** 收款方式 */
  private String paymentMethod;

  /** 收款方式名称 */
  private String paymentMethodName;

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

  /** 确认时间 */
  private LocalDateTime confirmedAt;

  /** 用途 */
  private String purpose;

  /** 备注 */
  private String remark;

  /** 核销记录列表 */
  private List<PrepaymentUsageDTO> usages;
}
