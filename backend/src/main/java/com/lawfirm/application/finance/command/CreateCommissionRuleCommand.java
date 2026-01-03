package com.lawfirm.application.finance.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建提成规则命令
 */
@Data
public class CreateCommissionRuleCommand {

    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    private String ruleType;

    @NotNull(message = "律所留存比例不能为空")
    private BigDecimal firmRetentionRate;

    @NotNull(message = "案源提成比例不能为空")
    private BigDecimal originatorRate;

    private BigDecimal taxRate;

    private BigDecimal managementFeeRate;

    private String rateTiers;

    private LocalDate effectiveDate;

    private LocalDate expiryDate;

    private Boolean isDefault;

    private Boolean active;
}

