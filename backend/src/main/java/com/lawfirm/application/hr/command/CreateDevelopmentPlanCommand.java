package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/** 创建发展规划命令 */
@Data
public class CreateDevelopmentPlanCommand {

  /** 规划年度 */
  @NotNull(message = "规划年度不能为空")
  private Integer planYear;

  /** 规划标题 */
  @NotBlank(message = "规划标题不能为空")
  private String planTitle;

  /** 目标级别ID */
  private Long targetLevelId;

  /** 目标日期 */
  private LocalDate targetDate;

  /** 职业目标列表 */
  private List<String> careerGoals;

  /** 技能目标列表 */
  private List<String> skillGoals;

  /** 绩效目标列表 */
  private List<String> performanceGoals;

  /** 行动计划列表 */
  private List<String> actionPlans;

  /** 所需培训 */
  private String requiredTraining;

  /** 所需资源 */
  private String requiredResources;

  /** 导师ID */
  private Long mentorId;

  /** 里程碑列表 */
  private List<MilestoneItem> milestones;

  /** 里程碑项 */
  @Data
  public static class MilestoneItem {
    /** 里程碑名称 */
    private String milestoneName;

    /** 描述 */
    private String description;

    /** 目标日期 */
    private LocalDate targetDate;

    /** 排序 */
    private Integer sortOrder;
  }
}
