package com.lawfirm.application.knowledge.command;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建质量检查标准命令（M10-030）
 */
@Data
public class CreateQualityCheckStandardCommand {
    private String standardName;
    private String category;
    private String description;
    private String checkItems; // JSON格式
    private String applicableMatterTypes;
    private BigDecimal weight;
    private Boolean enabled;
    private Integer sortOrder;
}

