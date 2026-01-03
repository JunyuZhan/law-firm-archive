package com.lawfirm.application.hr.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建考核指标命令
 */
@Data
public class CreateIndicatorCommand {

    private String name;
    private String code;
    private String category;
    private String description;
    private BigDecimal weight;
    private Integer maxScore;
    private String scoringCriteria;
    private String applicableRole;
    private Integer sortOrder;
    private String remarks;
}
