package com.lawfirm.application.hr.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人发展规划 DTO
 */
@Data
public class DevelopmentPlanDTO {
    private Long id;
    private String planNo;
    
    private Long employeeId;
    private String employeeName;
    
    private Integer planYear;
    private String planTitle;
    
    private Long currentLevelId;
    private String currentLevelName;
    private Long targetLevelId;
    private String targetLevelName;
    private LocalDate targetDate;
    
    private List<String> careerGoals;
    private List<String> skillGoals;
    private List<String> performanceGoals;
    private List<String> actionPlans;
    
    private String requiredTraining;
    private String requiredResources;
    private Long mentorId;
    private String mentorName;
    
    private Integer progressPercentage;
    private String progressNotes;
    
    private String status;
    private String statusName;
    
    private Long reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    
    /**
     * 里程碑列表
     */
    private List<DevelopmentMilestoneDTO> milestones;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
