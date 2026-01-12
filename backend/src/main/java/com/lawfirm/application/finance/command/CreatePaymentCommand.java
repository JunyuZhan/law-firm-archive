package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建收款记录命令
 */
@Data
public class CreatePaymentCommand {

    @NotNull(message = "收费ID不能为空")
    private Long feeId;

    @NotNull(message = "收款金额不能为空")
    @Positive(message = "收款金额必须大于0")
    private BigDecimal amount;

    private String currency;

    @NotBlank(message = "收款方式不能为空")
    private String paymentMethod;

    @NotNull(message = "收款日期不能为空")
    private LocalDate paymentDate;

    private String bankAccount;

    private String transactionNo;

    private String remark;
}

