package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/** 创建收款记录命令 */
@Data
public class CreatePaymentCommand {

  /** 收费ID */
  @NotNull(message = "收费ID不能为空")
  private Long feeId;

  /** 收款金额 */
  @NotNull(message = "收款金额不能为空")
  @Positive(message = "收款金额必须大于0")
  private BigDecimal amount;

  /** 币种 */
  private String currency;

  /** 收款方式 */
  @NotBlank(message = "收款方式不能为空")
  private String paymentMethod;

  /** 收款日期 */
  @NotNull(message = "收款日期不能为空")
  private LocalDate paymentDate;

  /** 银行账户 */
  private String bankAccount;

  /** 交易流水号 */
  private String transactionNo;

  /** 备注 */
  private String remark;
}
