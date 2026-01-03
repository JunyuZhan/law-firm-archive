package com.lawfirm.application.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 考核指标DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceIndicatorDTO {

    private Long id;
    private String name;
    private String code;
    private String category;
    private String categoryName;
    private String description;
    private BigDecimal weight;
    private Integer maxScore;
    private String scoringCriteria;
    private String applicableRole;
    private String applicableRoleName;
    private Integer sortOrder;
    private String status;
    private String remarks;
}
