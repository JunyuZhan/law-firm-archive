package com.lawfirm.application.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考核任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTaskDTO {

    private Long id;
    private String name;
    private String periodType;
    private String periodTypeName;
    private Integer year;
    private Integer period;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate selfEvalDeadline;
    private LocalDate peerEvalDeadline;
    private LocalDate supervisorEvalDeadline;
    private String status;
    private String statusName;
    private String description;
    private String remarks;
    private LocalDateTime createdAt;
    
    /**
     * 统计信息
     */
    private Integer totalEmployees;
    private Integer completedCount;
}
