package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 使用预收款命令（核销）
 */
@Data
public class UsePrepaymentCommand {

    @NotNull(message = "预收款ID不能为空")
    private Long prepaymentId;

    @NotNull(message = "收费记录ID不能为空")
    private Long feeId;

    @NotNull(message = "核销金额不能为空")
    @Positive(message = "核销金额必须大于0")
    private BigDecimal amount;

    private String remark;
}
