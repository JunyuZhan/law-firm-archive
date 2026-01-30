package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.dto.WorkbenchDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.ProjectSummary;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.RecentProjectDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.ScheduleItemDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.TimesheetSummary;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.TodoItemDTO;
import com.lawfirm.application.workbench.dto.WorkbenchDTO.TodoSummary;
import com.lawfirm.application.workbench.service.WorkbenchAppService;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个人工作台 Controller */
@Tag(name = "个人工作台", description = "个人工作台相关接口")
@RestController
@RequestMapping("/workbench")
@RequiredArgsConstructor
public class WorkbenchController {

  /** 工作台应用服务. */
  private final WorkbenchAppService workbenchAppService;

  /**
   * 获取工作台数据
   *
   * @return 工作台数据
   */
  @Operation(summary = "获取工作台数据")
  @GetMapping("/data")
  public Result<WorkbenchDTO> getWorkbenchData() {
    WorkbenchDTO data = workbenchAppService.getWorkbenchData();
    return Result.success(data);
  }

  /**
   * 获取待办事项统计
   *
   * @return 待办事项统计
   */
  @Operation(summary = "获取待办事项统计")
  @GetMapping("/todo/summary")
  public Result<TodoSummary> getTodoSummary() {
    Long userId = SecurityUtils.getCurrentUserId();
    TodoSummary summary = workbenchAppService.getTodoSummary(userId);
    return Result.success(summary);
  }

  /**
   * 获取待办事项列表
   *
   * @return 待办事项列表
   */
  @Operation(summary = "获取待办事项列表")
  @GetMapping("/todo/list")
  public Result<List<TodoItemDTO>> getTodoItems() {
    Long userId = SecurityUtils.getCurrentUserId();
    List<TodoItemDTO> items = workbenchAppService.getTodoItems(userId);
    return Result.success(items);
  }

  /**
   * 获取项目统计
   *
   * @return 项目统计
   */
  @Operation(summary = "获取项目统计")
  @GetMapping("/project/summary")
  public Result<ProjectSummary> getProjectSummary() {
    Long userId = SecurityUtils.getCurrentUserId();
    ProjectSummary summary = workbenchAppService.getProjectSummary(userId);
    return Result.success(summary);
  }

  /**
   * 获取最近项目
   *
   * @return 最近项目列表
   */
  @Operation(summary = "获取最近项目")
  @GetMapping("/project/recent")
  public Result<List<RecentProjectDTO>> getRecentProjects() {
    Long userId = SecurityUtils.getCurrentUserId();
    List<RecentProjectDTO> projects = workbenchAppService.getRecentProjects(userId);
    return Result.success(projects);
  }

  /**
   * 获取今日日程
   *
   * @return 今日日程列表
   */
  @Operation(summary = "获取今日日程")
  @GetMapping("/schedule/today")
  public Result<List<ScheduleItemDTO>> getTodaySchedules() {
    Long userId = SecurityUtils.getCurrentUserId();
    List<ScheduleItemDTO> schedules = workbenchAppService.getTodaySchedules(userId);
    return Result.success(schedules);
  }

  /**
   * 获取工时统计
   *
   * @return 工时统计
   */
  @Operation(summary = "获取本月工时统计")
  @GetMapping("/timesheet/summary")
  public Result<TimesheetSummary> getTimesheetSummary() {
    Long userId = SecurityUtils.getCurrentUserId();
    TimesheetSummary summary = workbenchAppService.getTimesheetSummary(userId);
    return Result.success(summary);
  }

  /**
   * 获取未读消息数量
   *
   * @return 未读消息数量
   */
  @Operation(summary = "获取未读消息数量")
  @GetMapping("/notification/unread-count")
  public Result<Integer> getUnreadNotificationCount() {
    Long userId = SecurityUtils.getCurrentUserId();
    int count = workbenchAppService.getUnreadNotificationCount(userId);
    return Result.success(count);
  }
}
