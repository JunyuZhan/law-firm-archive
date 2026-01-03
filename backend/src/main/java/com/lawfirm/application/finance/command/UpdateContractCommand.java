package com.lawfirm.application.finance.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新合同命令
 */
@Data
public class UpdateContractCommand {

    @NotNull(message = "合同ID不能为空")
    private Long id;

    private String name;
    private String contractType;
    private Long clientId;
    private Long matterId;
    private String feeType;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate signDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Long signerId;
    private Long departmentId;
    private String paymentTerms;
    private String fileUrl;
    private String remark;
}

