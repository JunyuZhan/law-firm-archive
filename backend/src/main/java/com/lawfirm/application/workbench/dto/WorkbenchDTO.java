package com.lawfirm.application.workbench.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工作台数据DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchDTO {

    /**
     * 待办事项统计
     */
    private TodoSummary todoSummary;

    /**
     * 我的项目统计
     */
    private ProjectSummary projectSummary;

    /**
     * 本月工时统计
     */
    private TimesheetSummary timesheetSummary;

    /**
     * 待办事项列表
     */
    private List<TodoItemDTO> todoItems;

    /**
     * 今日日程
     */
    private List<ScheduleItemDTO> todaySchedules;

    /**
     * 最近项目
     */
    private List<RecentProjectDTO> recentProjects;

    /**
     * 待办事项统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoSummary {
        private Integer pendingApproval;  // 待审批
        private Integer pendingTask;      // 待办任务
        private Integer overdueTask;      // 逾期任务
        private Integer total;            // 总计
    }

    /**
     * 项目统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSummary {
        private Integer inProgress;       // 进行中
        private Integer pendingApproval;  // 待审批
        private Integer closed;           // 已结案
        private Integer total;            // 总计
    }

    /**
     * 工时统计
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimesheetSummary {
        private BigDecimal totalHours;    // 本月总工时
        private BigDecimal billableHours; // 可计费工时
        private Integer recordCount;      // 记录数
    }

    /**
     * 待办事项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodoItemDTO {
        private Long id;
        private String type;          // APPROVAL, TASK, SCHEDULE
        private String title;
        private String description;
        private String priority;      // HIGH, MEDIUM, LOW
        private String dueDate;
        private String status;
        private String relatedId;     // 关联业务ID
        private String relatedType;   // 关联业务类型
    }

    /**
     * 日程项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleItemDTO {
        private Long id;
        private String title;
        private String startTime;
        private String endTime;
        private String type;          // COURT, MEETING, DEADLINE, OTHER
        private String location;
        private Long matterId;
        private String matterName;
    }

    /**
     * 最近项目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentProjectDTO {
        private Long id;
        private String matterNo;
        private String matterName;
        private String clientName;
        private String status;
        private String role;          // 我在项目中的角色
        private String lastUpdateTime;
    }
}
