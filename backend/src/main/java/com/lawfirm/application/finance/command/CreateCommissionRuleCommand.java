package com.lawfirm.application.finance.command;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

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

    /** 律所比例(%)，0表示不参与分配 */
    private BigDecimal firmRate;

    /** 主办律师比例(%)，0表示不参与分配 */
    private BigDecimal leadLawyerRate;

    /** 协办律师比例(%)，0表示无协办或不参与分配 */
    private BigDecimal assistLawyerRate;

    /** 辅助人员比例(%)，0表示无辅助或不参与分配 */
    private BigDecimal supportStaffRate;

    /** 案源人比例(%)，0表示不参与分配 */
    private BigDecimal originatorRate;

    /** 律师创建合同时是否允许修改比例 */
    private Boolean allowModify;

    private String description;

    private Boolean isDefault;

    private Boolean active;
}
