package com.lawfirm.application.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 绩效评分明细DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceScoreDTO {

    private Long id;
    private Long evaluationId;
    private Long indicatorId;
    private String indicatorName;
    private String indicatorCategory;
    private BigDecimal weight;
    private Integer maxScore;
    private BigDecimal score;
    private String comment;
}
