package com.lawfirm.application.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 个人发展规划 DTO */
@Data
public class DevelopmentPlanDTO {
  /** 规划ID */
  private Long id;

  /** 规划编号 */
  private String planNo;

  /** 员工ID */
  private Long employeeId;

  /** 员工姓名 */
  private String employeeName;

  /** 规划年度 */
  private Integer planYear;

  /** 规划标题 */
  private String planTitle;

  /** 当前级别ID */
  private Long currentLevelId;

  /** 当前级别名称 */
  private String currentLevelName;

  /** 目标级别ID */
  private Long targetLevelId;

  /** 目标级别名称 */
  private String targetLevelName;

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

  /** 导师姓名 */
  private String mentorName;

  /** 进度百分比 */
  private Integer progressPercentage;

  /** 进度说明 */
  private String progressNotes;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审核人ID */
  private Long reviewedBy;

  /** 审核人姓名 */
  private String reviewedByName;

  /** 审核时间 */
  private LocalDateTime reviewedAt;

  /** 审核意见 */
  private String reviewComment;

  /** 里程碑列表 */
  private List<DevelopmentMilestoneDTO> milestones;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
