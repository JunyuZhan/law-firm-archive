package com.lawfirm.application.hr.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 发展规划里程碑 DTO
 */
@Data
public class DevelopmentMilestoneDTO {
    private Long id;
    private Long planId;
    
    private String milestoneName;
    private String description;
    private LocalDate targetDate;
    
    private String status;
    private String statusName;
    
    private LocalDate completedDate;
    private String completionNote;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
