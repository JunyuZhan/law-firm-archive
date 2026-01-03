package com.lawfirm.application.workbench.service;

import com.lawfirm.application.workbench.dto.WorkbenchDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.*;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.domain.matter.entity.Task;
import com.lawfirm.domain.matter.repository.ScheduleRepository;
import com.lawfirm.domain.matter.repository.TaskRepository;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作台应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbenchAppService {

    private final ApprovalRepository approvalRepository;
    private final TaskRepository taskRepository;
    private final ScheduleRepository scheduleRepository;
    private final MatterMapper matterMapper;
    private final TimesheetMapper timesheetMapper;

    /**
     * 获取工作台数据
     */
    public WorkbenchDTO getWorkbenchData() {
        Long userId = SecurityUtils.getCurrentUserId();
        
        return WorkbenchDTO.builder()
                .todoSummary(getTodoSummary(userId))
                .projectSummary(getProjectSummary(userId))
                .timesheetSummary(getTimesheetSummary(userId))
                .todoItems(getTodoItems(userId))
                .todaySchedules(getTodaySchedules(userId))
                .recentProjects(getRecentProjects(userId))
                .build();
    }

    /**
     * 获取待办事项统计
     */
    public TodoSummary getTodoSummary(Long userId) {
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
    }

    /**
     * 获取项目统计
     */
    public ProjectSummary getProjectSummary(Long userId) {
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
     */
    public TimesheetSummary getTimesheetSummary(Long userId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        BigDecimal totalHours = timesheetMapper.sumHoursByUserIdAndDateRange(
                userId, startOfMonth, endOfMonth);
        BigDecimal billableHours = timesheetMapper.sumBillableHoursByUserIdAndDateRange(
                userId, startOfMonth, endOfMonth);
        int recordCount = timesheetMapper.countByUserIdAndDateRange(
                userId, startOfMonth, endOfMonth);
        
        return TimesheetSummary.builder()
                .totalHours(totalHours != null ? totalHours : BigDecimal.ZERO)
                .billableHours(billableHours != null ? billableHours : BigDecimal.ZERO)
                .recordCount(recordCount)
                .build();
    }

    /**
     * 获取待办事项列表
     */
    public List<TodoItemDTO> getTodoItems(Long userId) {
        List<TodoItemDTO> items = new ArrayList<>();
        
        // 获取待审批事项
        approvalRepository.findPendingByApproverId(userId, 5).forEach(approval -> {
            items.add(TodoItemDTO.builder()
                    .id(approval.getId())
                    .type("APPROVAL")
                    .title(approval.getBusinessTitle())
                    .description(approval.getBusinessType())
                    .priority("HIGH")
                    .dueDate(approval.getCreatedAt() != null ? 
                            approval.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
                    .status("PENDING")
                    .relatedId(approval.getBusinessId().toString())
                    .relatedType(approval.getBusinessType())
                    .build());
        });
        
        // 获取待办任务
        taskRepository.findPendingByAssigneeId(userId, 5).forEach(task -> {
            items.add(TodoItemDTO.builder()
                    .id(task.getId())
                    .type("TASK")
                    .title(task.getTitle())
                    .description(task.getDescription())
                    .priority(task.getPriority())
                    .dueDate(task.getDueDate() != null ? 
                            task.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE) : null)
                    .status(task.getStatus())
                    .relatedId(task.getMatterId() != null ? task.getMatterId().toString() : null)
                    .relatedType("MATTER")
                    .build());
        });
        
        return items;
    }

    /**
     * 获取今日日程
     */
    public List<ScheduleItemDTO> getTodaySchedules(Long userId) {
        LocalDate today = LocalDate.now();
        List<Schedule> schedules = scheduleRepository.findByUserAndDate(userId, today);
        
        return schedules.stream()
                .map(s -> ScheduleItemDTO.builder()
                        .id(s.getId())
                        .title(s.getTitle())
                        .startTime(s.getStartTime() != null ? 
                                s.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null)
                        .endTime(s.getEndTime() != null ? 
                                s.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")) : null)
                        .type(s.getScheduleType())
                        .location(s.getLocation())
                        .matterId(s.getMatterId())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取最近项目
     */
    public List<RecentProjectDTO> getRecentProjects(Long userId) {
        return matterMapper.selectRecentByUserId(userId, 5).stream()
                .map(m -> RecentProjectDTO.builder()
                        .id(m.getId())
                        .matterNo(m.getMatterNo())
                        .matterName(m.getName())
                        .status(m.getStatus())
                        .lastUpdateTime(m.getUpdatedAt() != null ? 
                                m.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取消息通知数量
     */
    public int getUnreadNotificationCount(Long userId) {
        // TODO: 实现消息通知统计
        return 0;
    }
}
