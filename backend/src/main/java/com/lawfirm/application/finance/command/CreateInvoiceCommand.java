package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建发票命令
 */
@Data
public class CreateInvoiceCommand {

    private Long feeId;

    private Long contractId;

    @NotNull(message = "客户不能为空")
    private Long clientId;

    @NotBlank(message = "发票类型不能为空")
    private String invoiceType;

    @NotBlank(message = "发票抬头不能为空")
    private String title;

    private String taxNo;

    @NotNull(message = "开票金额不能为空")
    private BigDecimal amount;

    private BigDecimal taxRate;

    private String content;

    private String remark;
}

