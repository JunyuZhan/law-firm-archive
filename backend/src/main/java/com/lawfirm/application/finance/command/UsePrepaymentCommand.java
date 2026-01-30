package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Data;

/** 使用预收款命令（核销） */
@Data
public class UsePrepaymentCommand {

  /** 预收款ID */
  @NotNull(message = "预收款ID不能为空")
  private Long prepaymentId;

  /** 收费记录ID */
  @NotNull(message = "收费记录ID不能为空")
  private Long feeId;

  /** 核销金额 */
  @NotNull(message = "核销金额不能为空")
  @Positive(message = "核销金额必须大于0")
  private BigDecimal amount;

  /** 备注 */
  private String remark;
}
