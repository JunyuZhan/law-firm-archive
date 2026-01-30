package com.lawfirm.application.workbench.service;

import com.lawfirm.application.workbench.dto.WorkbenchDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.ProjectSummary;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.RecentProjectDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.ScheduleItemDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.TimesheetSummary;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.TodoItemDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.TodoSummary;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.domain.matter.repository.ScheduleRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 工作台应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbenchAppService {

  /** 审批仓储 */
  private final ApprovalRepository approvalRepository;

  /** 任务仓储 */
  private final TaskRepository taskRepository;

  /** 日程仓储 */
  private final ScheduleRepository scheduleRepository;

  /** 项目Mapper */
  private final MatterMapper matterMapper;

  /** 工时Mapper */
  private final TimesheetMapper timesheetMapper;

  /** 通知Mapper */
  private final com.lawfirm.infrastructure.persistence.mapper.NotificationMapper notificationMapper;

  /**
   * 获取工作台数据 ✅ 修复问题555: 分块异常处理，允许部分数据加载失败但保留其他数据
   *
   * @return 工作台数据
   */
  public WorkbenchDTO getWorkbenchData() {
    Long userId = SecurityUtils.getCurrentUserId();
    WorkbenchDTO.WorkbenchDTOBuilder builder = WorkbenchDTO.builder();

    // ✅ 分块处理：每个模块独立异常处理，部分失败不影响其他模块
    try {
      builder.todoSummary(getTodoSummary(userId));
    } catch (Exception e) {
      log.error("获取待办事项统计失败", e);
      builder.todoSummary(
          TodoSummary.builder().pendingApproval(0).pendingTask(0).overdueTask(0).total(0).build());
    }

    try {
      builder.projectSummary(getProjectSummary(userId));
    } catch (Exception e) {
      log.error("获取项目统计失败", e);
      builder.projectSummary(
          ProjectSummary.builder().inProgress(0).pendingApproval(0).closed(0).total(0).build());
    }

    try {
      builder.timesheetSummary(getTimesheetSummary(userId));
    } catch (Exception e) {
      log.error("获取工时统计失败", e);
      builder.timesheetSummary(
          TimesheetSummary.builder()
              .totalHours(BigDecimal.ZERO)
              .billableHours(BigDecimal.ZERO)
              .recordCount(0)
              .build());
    }

    try {
      builder.todoItems(getTodoItems(userId));
    } catch (Exception e) {
      log.error("获取待办事项列表失败", e);
      builder.todoItems(new ArrayList<>());
    }

    try {
      builder.todaySchedules(getTodaySchedules(userId));
    } catch (Exception e) {
      log.error("获取今日日程失败", e);
      builder.todaySchedules(new ArrayList<>());
    }

    try {
      builder.recentProjects(getRecentProjects(userId));
    } catch (Exception e) {
      log.error("获取最近项目失败", e);
      builder.recentProjects(new ArrayList<>());
    }

    return builder.build();
  }

  /**
   * 获取待办事项统计
   *
   * @param userId 用户ID
   * @return 待办事项统计
   */
  public TodoSummary getTodoSummary(final Long userId) {
    try {
      // 待审批数量
      int pendingApproval = approvalRepository.countPendingByApproverId(userId);

      // 待办任务数量
      int pendingTask = taskRepository.countPendingByAssigneeId(userId);

      // 逾期任务数量
      int overdueTask = taskRepository.countOverdueByAssigneeId(userId);

      return TodoSummary.builder()
          .pendingApproval(pendingApproval)
          .pendingTask(pendingTask)
          .overdueTask(overdueTask)
          .total(pendingApproval + pendingTask)
          .build();
    } catch (Exception e) {
      log.error("获取待办事项统计失败: userId={}", userId, e);
      // 返回空统计，避免500错误
      return TodoSummary.builder()
          .pendingApproval(0)
          .pendingTask(0)
          .overdueTask(0)
          .total(0)
          .build();
    }
  }

  /**
   * 获取项目统计
   *
   * @param userId 用户ID
   * @return 项目统计
   */
  public ProjectSummary getProjectSummary(final Long userId) {
    // 统计用户参与的项目
    int inProgress = matterMapper.countByUserIdAndStatus(userId, "IN_PROGRESS");
    int pendingApproval = matterMapper.countByUserIdAndStatus(userId, "PENDING_APPROVAL");
    int closed = matterMapper.countByUserIdAndStatus(userId, "CLOSED");

    return ProjectSummary.builder()
        .inProgress(inProgress)
        .pendingApproval(pendingApproval)
        .closed(closed)
        .total(inProgress + pendingApproval + closed)
        .build();
  }

  /**
   * 获取本月工时统计
   *
   * @param userId 用户ID
   * @return 工时统计
   */
  public TimesheetSummary getTimesheetSummary(final Long userId) {
    LocalDate now = LocalDate.now();
    LocalDate startOfMonth = now.withDayOfMonth(1);
    LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

    BigDecimal totalHours =
        timesheetMapper.sumHoursByUserIdAndDateRange(userId, startOfMonth, endOfMonth);
    BigDecimal billableHours =
        timesheetMapper.sumBillableHoursByUserIdAndDateRange(userId, startOfMonth, endOfMonth);
    int recordCount = timesheetMapper.countByUserIdAndDateRange(userId, startOfMonth, endOfMonth);

    return TimesheetSummary.builder()
        .totalHours(totalHours != null ? totalHours : BigDecimal.ZERO)
        .billableHours(billableHours != null ? billableHours : BigDecimal.ZERO)
        .recordCount(recordCount)
        .build();
  }

  /**
   * 获取待办事项列表
   *
   * @param userId 用户ID
   * @return 待办事项列表
   */
  public List<TodoItemDTO> getTodoItems(final Long userId) {
    List<TodoItemDTO> items = new ArrayList<>();

    // 获取待审批事项
    approvalRepository
        .findPendingByApproverId(userId, 5)
        .forEach(
            approval -> {
              items.add(
                  TodoItemDTO.builder()
                      .id(approval.getId())
                      .type("APPROVAL")
                      .title(approval.getBusinessTitle())
                      .description(approval.getBusinessType())
                      .priority("HIGH")
                      .dueDate(
                          approval.getCreatedAt() != null
                              ? approval.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                              : null)
                      .status("PENDING")
                      .relatedId(approval.getBusinessId().toString())
                      .relatedType(approval.getBusinessType())
                      .build());
            });

    // 获取待办任务
    taskRepository
        .findPendingByAssigneeId(userId, 5)
        .forEach(
            task -> {
              items.add(
                  TodoItemDTO.builder()
                      .id(task.getId())
                      .type("TASK")
                      .title(task.getTitle())
                      .description(task.getDescription())
                      .priority(task.getPriority())
                      .dueDate(
                          task.getDueDate() != null
                              ? task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                              : null)
                      .status(task.getStatus())
                      .relatedId(task.getMatterId() != null ? task.getMatterId().toString() : null)
                      .relatedType("MATTER")
                      .build());
            });

    return items;
  }

  /**
   * 获取今日日程
   *
   * @param userId 用户ID
   * @return 今日日程列表
   */
  public List<ScheduleItemDTO> getTodaySchedules(final Long userId) {
    LocalDate today = LocalDate.now();
    List<Schedule> schedules = scheduleRepository.findByUserAndDate(userId, today);

    return schedules.stream()
        .map(
            s ->
                ScheduleItemDTO.builder()
                    .id(s.getId())
                    .title(s.getTitle())
                    .startTime(
                        s.getStartTime() != null
                            ? s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            : null)
                    .endTime(
                        s.getEndTime() != null
                            ? s.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                            : null)
                    .type(s.getScheduleType())
                    .location(s.getLocation())
                    .matterId(s.getMatterId())
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * 获取最近项目
   *
   * @param userId 用户ID
   * @return 最近项目列表
   */
  public List<RecentProjectDTO> getRecentProjects(final Long userId) {
    return matterMapper.selectRecentByUserId(userId, 5).stream()
        .map(
            m ->
                RecentProjectDTO.builder()
                    .id(m.getId())
                    .matterNo(m.getMatterNo())
                    .matterName(m.getName())
                    .status(m.getStatus())
                    .lastUpdateTime(
                        m.getUpdatedAt() != null
                            ? m.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            : null)
                    .build())
        .collect(Collectors.toList());
  }

  /**
   * 获取消息通知数量
   *
   * @param userId 用户ID
   * @return 未读消息数量
   */
  public int getUnreadNotificationCount(final Long userId) {
    try {
      return notificationMapper.countUnread(userId);
    } catch (Exception e) {
      log.error("查询未读消息数量失败", e);
      return 0;
    }
  }
}
