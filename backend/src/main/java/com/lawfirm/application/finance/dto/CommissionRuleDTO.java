package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 提成规则 DTO
 */
@Data
public class CommissionRuleDTO {

    private Long id;
    private String ruleCode;
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

