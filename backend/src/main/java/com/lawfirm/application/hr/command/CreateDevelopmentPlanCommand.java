package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建发展规划命令
 */
@Data
public class CreateDevelopmentPlanCommand {
    
    @NotNull(message = "规划年度不能为空")
    private Integer planYear;
    
    @NotBlank(message = "规划标题不能为空")
    private String planTitle;
    
    private Long targetLevelId;
    private LocalDate targetDate;
    
    private List<String> careerGoals;
    private List<String> skillGoals;
    private List<String> performanceGoals;
    private List<String> actionPlans;
    
    private String requiredTraining;
    private String requiredResources;
    private Long mentorId;
    
    /**
     * 里程碑列表
     */
    private List<MilestoneItem> milestones;
    
    @Data
    public static class MilestoneItem {
        private String milestoneName;
        private String description;
        private LocalDate targetDate;
        private Integer sortOrder;
    }
}
