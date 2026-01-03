package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 质量检查标准DTO（M10-030）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckStandardDTO extends BaseDTO {
    private String standardNo;
    private String standardName;
    private String category;
    private String categoryName;
    private String description;
    private String checkItems;
    private String applicableMatterTypes;
    private BigDecimal weight;
    private Boolean enabled;
    private Integer sortOrder;
}

