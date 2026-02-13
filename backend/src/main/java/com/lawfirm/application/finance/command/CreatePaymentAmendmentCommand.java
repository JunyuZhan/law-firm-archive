package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 创建收款变更申请命令
 *
 * <p>Requirements: 3.5
 */
@Data
public class CreatePaymentAmendmentCommand {

  /** 收款记录ID */
  @NotNull(message = "收款记录ID不能为空")
  private Long paymentId;

  /** 新金额 */
  @NotNull(message = "新金额不能为空")
  @Positive(message = "新金额必须大于0")
  private BigDecimal newAmount;

  /** 变更原因 */
  @NotNull(message = "变更原因不能为空")
  private String reason;
}
