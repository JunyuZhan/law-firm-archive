package com.lawfirm.application.finance.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新提成规则命令
 */
@Data
public class UpdateCommissionRuleCommand {

    private String ruleName;
    private String ruleType;
    private BigDecimal firmRetentionRate;
    private BigDecimal originatorRate;
    private BigDecimal taxRate;
    private BigDecimal managementFeeRate;
    private String rateTiers;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private Boolean isDefault;
    private Boolean active;
}

