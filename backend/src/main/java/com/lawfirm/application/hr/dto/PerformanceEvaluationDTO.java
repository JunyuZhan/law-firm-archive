package com.lawfirm.application.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 绩效评价DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceEvaluationDTO {

    private Long id;
    private Long taskId;
    private String taskName;
    private Long employeeId;
    private String employeeName;
    private Long evaluatorId;
    private String evaluatorName;
    private String evaluationType;
    private String evaluationTypeName;
    private BigDecimal totalScore;
    private String grade;
    private String gradeName;
    private String comment;
    private String strengths;
    private String improvements;
    private LocalDateTime evaluatedAt;
    private String status;
    private String statusName;
    
    /**
     * 评分明细
     */
    private List<PerformanceScoreDTO> scores;
}
