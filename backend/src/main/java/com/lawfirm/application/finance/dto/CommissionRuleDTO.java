package com.lawfirm.application.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 提成规则 DTO
 */
@Data
public class CommissionRuleDTO {

    private Long id;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private BigDecimal firmRate;
    private BigDecimal leadLawyerRate;
    private BigDecimal assistLawyerRate;
    private BigDecimal supportStaffRate;
    private BigDecimal originatorRate;
    private Boolean allowModify;
    private String description;
    private Boolean isDefault;
    private Boolean active;
}
