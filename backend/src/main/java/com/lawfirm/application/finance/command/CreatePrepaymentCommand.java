package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建预收款命令 */
@Data
public class CreatePrepaymentCommand {

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long clientId;

  /** 合同ID */
  private Long contractId;

  /** 案件ID */
  private Long matterId;

  /** 预收款金额 */
  @NotNull(message = "预收款金额不能为空")
  @Positive(message = "预收款金额必须大于0")
  private BigDecimal amount;

  /** 币种 */
  private String currency;

  /** 收款日期 */
  @NotNull(message = "收款日期不能为空")
  private LocalDate receiptDate;

  /** 收款方式 */
  private String paymentMethod;

  /** 银行账户 */
  private String bankAccount;

  /** 交易流水号 */
  private String transactionNo;

  /** 用途 */
  private String purpose;

  /** 备注 */
  private String remark;
}
