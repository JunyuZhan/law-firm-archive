package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateTaskCommand;
import com.lawfirm.application.matter.dto.TaskDTO;
import com.lawfirm.application.matter.dto.TaskQueryDTO;
import com.lawfirm.application.matter.service.TaskAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 任务管理接口 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

  /** 任务应用服务 */
  private final TaskAppService taskAppService;

  /**
   * 分页查询任务
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("task:list")
  public Result<PageResult<TaskDTO>> list(final TaskQueryDTO query) {
    return Result.success(taskAppService.listTasks(query));
  }

  /**
   * 获取任务详情
   *
   * @param id 任务ID
   * @return 任务信息
   */
  @GetMapping("/{id}")
  @RequirePermission("task:view")
  public Result<TaskDTO> getById(@PathVariable final Long id) {
    return Result.success(taskAppService.getTaskById(id));
  }

  /**
   * 创建任务
   *
   * @param command 创建任务命令
   * @return 任务信息
   */
  @PostMapping
  @RequirePermission("task:manage")
  @OperationLog(module = "任务管理", action = "创建任务")
  public Result<TaskDTO> create(@Valid @RequestBody final CreateTaskCommand command) {
    return Result.success(taskAppService.createTask(command));
  }

  /**
   * 更新任务
   *
   * @param id 任务ID
   * @param title 标题
   * @param description 描述
   * @param priority 优先级
   * @param assigneeId 执行人ID
   * @param assigneeName 执行人姓名
   * @param startDate 开始日期
   * @param dueDate 截止日期
   * @param reminderDate 提醒日期
   * @return 任务信息
   */
  @PutMapping("/{id}")
  @RequirePermission("task:manage")
  @OperationLog(module = "任务管理", action = "更新任务")
  public Result<TaskDTO> update(
      @PathVariable final Long id,
      @RequestParam(required = false) final String title,
      @RequestParam(required = false) final String description,
      @RequestParam(required = false) final String priority,
      @RequestParam(required = false) final Long assigneeId,
      @RequestParam(required = false) final String assigneeName,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          final LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          final LocalDate dueDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          final LocalDateTime reminderDate) {
    return Result.success(
        taskAppService.updateTask(
            id,
            title,
            description,
            priority,
            assigneeId,
            assigneeName,
            startDate,
            dueDate,
            reminderDate));
  }

  /**
   * 删除任务
   *
   * @param id 任务ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("task:manage")
  @OperationLog(module = "任务管理", action = "删除任务")
  public Result<Void> delete(@PathVariable final Long id) {
    taskAppService.deleteTask(id);
    return Result.success();
  }

  /**
   * 更新任务状态
   *
   * @param id 任务ID
   * @param status 状态
   * @return 任务信息
   */
  @PutMapping("/{id}/status")
  @RequirePermission("task:manage")
  @OperationLog(module = "任务管理", action = "更新任务状态")
  public Result<TaskDTO> updateStatus(
      @PathVariable final Long id, @RequestParam final String status) {
    return Result.success(taskAppService.updateStatus(id, status));
  }

  /**
   * 更新任务进度
   *
   * @param id 任务ID
   * @param progress 进度
   * @return 任务信息
   */
  @PutMapping("/{id}/progress")
  @RequirePermission("task:manage")
  public Result<TaskDTO> updateProgress(
      @PathVariable final Long id,
      @RequestParam @jakarta.validation.constraints.Min(value = 0, message = "进度不能小于0")
          @jakarta.validation.constraints.Max(value = 100, message = "进度不能大于100")
          final Integer progress) {
    return Result.success(taskAppService.updateProgress(id, progress));
  }

  /**
   * 获取我的任务（支持分页和状态过滤） 注意：此接口仅返回当前用户自己的任务，无需特殊权限
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/my")
  public Result<PageResult<TaskDTO>> getMyTasks(final TaskQueryDTO query) {
    // 设置当前用户为执行人
    query.setAssigneeId(com.lawfirm.common.util.SecurityUtils.getUserId());
    return Result.success(taskAppService.listTasks(query));
  }

  /**
   * 获取我的待办任务 注意：此接口仅返回当前用户自己的待办任务，无需特殊权限
   *
   * @return 待办任务列表
   */
  @GetMapping("/my/todo")
  public Result<List<TaskDTO>> getMyTodo() {
    return Result.success(taskAppService.getMyTodoTasks());
  }

  /**
   * 获取即将到期的任务
   *
   * @param days 天数
   * @return 即将到期的任务列表
   */
  @GetMapping("/upcoming")
  @RequirePermission("task:list")
  public Result<List<TaskDTO>> getUpcoming(@RequestParam(defaultValue = "7") final int days) {
    return Result.success(taskAppService.getUpcomingTasks(days));
  }

  /**
   * 获取逾期任务
   *
   * @return 逾期任务列表
   */
  @GetMapping("/overdue")
  @RequirePermission("task:list")
  public Result<List<TaskDTO>> getOverdue() {
    return Result.success(taskAppService.getOverdueTasks());
  }

  /**
   * 获取案件任务统计
   *
   * @param matterId 案件ID
   * @return 任务统计数组
   */
  @GetMapping("/stats/matter/{matterId}")
  @RequirePermission("task:list")
  public Result<int[]> getMatterStats(@PathVariable final Long matterId) {
    return Result.success(taskAppService.getMatterTaskStats(matterId));
  }

  /**
   * 验收任务（通过）
   *
   * @param id 任务ID
   * @return 任务信息
   */
  @PostMapping("/{id}/review/approve")
  @RequirePermission("task:manage")
  @OperationLog(module = "任务管理", action = "验收任务通过")
  public Result<TaskDTO> approveTask(@PathVariable final Long id) {
    return Result.success(taskAppService.approveTask(id));
  }

  /**
   * 验收任务（退回）
   *
   * @param id 任务ID
   * @param comment 退回意见
   * @return 任务信息
   */
  @PostMapping("/{id}/review/reject")
  @RequirePermission("task:manage")
  @OperationLog(module = "任务管理", action = "验收任务退回")
  public Result<TaskDTO> rejectTask(
      @PathVariable final Long id, @RequestParam final String comment) {
    return Result.success(taskAppService.rejectTask(id, comment));
  }
}
