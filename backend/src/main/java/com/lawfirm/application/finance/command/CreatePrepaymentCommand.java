package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建预收款命令
 */
@Data
public class CreatePrepaymentCommand {

    @NotNull(message = "客户ID不能为空")
    private Long clientId;

    private Long contractId;

    private Long matterId;

    @NotNull(message = "预收款金额不能为空")
    @Positive(message = "预收款金额必须大于0")
    private BigDecimal amount;

    private String currency;

    @NotNull(message = "收款日期不能为空")
    private LocalDate receiptDate;

    private String paymentMethod;

    private String bankAccount;

    private String transactionNo;

    private String purpose;

    private String remark;
}
