package com.lawfirm.application.workbench.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 工作台数据DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchDTO {

  /** 待办事项统计 */
  private TodoSummary todoSummary;

  /** 我的项目统计 */
  private ProjectSummary projectSummary;

  /** 本月工时统计 */
  private TimesheetSummary timesheetSummary;

  /** 待办事项列表 */
  private List<TodoItemDTO> todoItems;

  /** 今日日程 */
  private List<ScheduleItemDTO> todaySchedules;

  /** 最近项目 */
  private List<RecentProjectDTO> recentProjects;

  /** 待办事项统计 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TodoSummary {
    /** 待审批 */
    private Integer pendingApproval;

    /** 待办任务 */
    private Integer pendingTask;

    /** 逾期任务 */
    private Integer overdueTask;

    /** 总计 */
    private Integer total;
  }

  /** 项目统计 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProjectSummary {
    /** 进行中 */
    private Integer inProgress;

    /** 待审批 */
    private Integer pendingApproval;

    /** 已结案 */
    private Integer closed;

    /** 总计 */
    private Integer total;
  }

  /** 工时统计 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimesheetSummary {
    /** 本月总工时 */
    private BigDecimal totalHours;

    /** 可计费工时 */
    private BigDecimal billableHours;

    /** 记录数 */
    private Integer recordCount;
  }

  /** 待办事项 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TodoItemDTO {
    /** 事项ID */
    private Long id;

    /** 类型（APPROVAL, TASK, SCHEDULE） */
    private String type;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 优先级（HIGH, MEDIUM, LOW） */
    private String priority;

    /** 截止日期 */
    private String dueDate;

    /** 状态 */
    private String status;

    /** 关联业务ID */
    private String relatedId;

    /** 关联业务类型 */
    private String relatedType;
  }

  /** 日程项 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScheduleItemDTO {
    /** 日程ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 类型（COURT, MEETING, DEADLINE, OTHER） */
    private String type;

    /** 地点 */
    private String location;

    /** 案件ID */
    private Long matterId;

    /** 案件名称 */
    private String matterName;
  }

  /** 最近项目 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecentProjectDTO {
    /** 项目ID */
    private Long id;

    /** 项目编号 */
    private String matterNo;

    /** 项目名称 */
    private String matterName;

    /** 客户名称 */
    private String clientName;

    /** 状态 */
    private String status;

    /** 我在项目中的角色 */
    private String role;

    /** 最后更新时间 */
    private String lastUpdateTime;
  }
}
