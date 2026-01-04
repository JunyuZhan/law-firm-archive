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
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务管理接口
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskAppService taskAppService;

    /**
     * 分页查询任务
     */
    @GetMapping
    @RequirePermission("task:list")
    public Result<PageResult<TaskDTO>> list(TaskQueryDTO query) {
        return Result.success(taskAppService.listTasks(query));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    @RequirePermission("task:view")
    public Result<TaskDTO> getById(@PathVariable Long id) {
        return Result.success(taskAppService.getTaskById(id));
    }

    /**
     * 创建任务
     */
    @PostMapping
    @RequirePermission("task:manage")
    @OperationLog(module = "任务管理", action = "创建任务")
    public Result<TaskDTO> create(@Valid @RequestBody CreateTaskCommand command) {
        return Result.success(taskAppService.createTask(command));
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    @RequirePermission("task:manage")
    @OperationLog(module = "任务管理", action = "更新任务")
    public Result<TaskDTO> update(@PathVariable Long id,
                                  @RequestParam(required = false) String title,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String priority,
                                  @RequestParam(required = false) Long assigneeId,
                                  @RequestParam(required = false) String assigneeName,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reminderDate) {
        return Result.success(taskAppService.updateTask(id, title, description, priority,
                assigneeId, assigneeName, startDate, dueDate, reminderDate));
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    @RequirePermission("task:manage")
    @OperationLog(module = "任务管理", action = "删除任务")
    public Result<Void> delete(@PathVariable Long id) {
        taskAppService.deleteTask(id);
        return Result.success();
    }

    /**
     * 更新任务状态
     */
    @PutMapping("/{id}/status")
    @RequirePermission("task:manage")
    @OperationLog(module = "任务管理", action = "更新任务状态")
    public Result<TaskDTO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.success(taskAppService.updateStatus(id, status));
    }

    /**
     * 更新任务进度
     */
    @PutMapping("/{id}/progress")
    @RequirePermission("task:manage")
    public Result<TaskDTO> updateProgress(@PathVariable Long id, @RequestParam Integer progress) {
        return Result.success(taskAppService.updateProgress(id, progress));
    }

    /**
     * 获取我的任务（支持分页和状态过滤）
     */
    @GetMapping("/my")
    @RequirePermission("task:view")
    public Result<PageResult<TaskDTO>> getMyTasks(TaskQueryDTO query) {
        // 设置当前用户为执行人
        query.setAssigneeId(com.lawfirm.common.util.SecurityUtils.getUserId());
        return Result.success(taskAppService.listTasks(query));
    }

    /**
     * 获取我的待办任务
     */
    @GetMapping("/my/todo")
    @RequirePermission("task:view")
    public Result<List<TaskDTO>> getMyTodo() {
        return Result.success(taskAppService.getMyTodoTasks());
    }

    /**
     * 获取即将到期的任务
     */
    @GetMapping("/upcoming")
    @RequirePermission("task:list")
    public Result<List<TaskDTO>> getUpcoming(@RequestParam(defaultValue = "7") int days) {
        return Result.success(taskAppService.getUpcomingTasks(days));
    }

    /**
     * 获取逾期任务
     */
    @GetMapping("/overdue")
    @RequirePermission("task:list")
    public Result<List<TaskDTO>> getOverdue() {
        return Result.success(taskAppService.getOverdueTasks());
    }

    /**
     * 获取案件任务统计
     */
    @GetMapping("/stats/matter/{matterId}")
    @RequirePermission("task:list")
    public Result<int[]> getMatterStats(@PathVariable Long matterId) {
        return Result.success(taskAppService.getMatterTaskStats(matterId));
    }
}
