package com.lawfirm.interfaces.rest.hr;

import com.lawfirm.application.hr.command.CreateIndicatorCommand;
import com.lawfirm.application.hr.command.CreatePerformanceTaskCommand;
import com.lawfirm.application.hr.command.SubmitEvaluationCommand;
import com.lawfirm.application.hr.dto.PerformanceEvaluationDTO;
import com.lawfirm.application.hr.dto.PerformanceIndicatorDTO;
import com.lawfirm.application.hr.dto.PerformanceTaskDTO;
import com.lawfirm.application.hr.service.PerformanceAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 绩效考核接口
 */
@Tag(name = "绩效考核", description = "绩效考核管理相关接口")
@RestController
@RequestMapping("/hr/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceAppService performanceAppService;

    // ==================== 考核任务 ====================

    @Operation(summary = "分页查询考核任务")
    @GetMapping("/tasks")
    @RequirePermission("hr:performance:list")
    public Result<PageResult<PerformanceTaskDTO>> listTasks(
            PageQuery query,
            @Parameter(description = "年份") @RequestParam(required = false) Integer year,
            @Parameter(description = "周期类型") @RequestParam(required = false) String periodType,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(performanceAppService.listTasks(query, year, periodType, status));
    }

    @Operation(summary = "获取考核任务详情")
    @GetMapping("/tasks/{id}")
    @RequirePermission("hr:performance:detail")
    public Result<PerformanceTaskDTO> getTaskById(@PathVariable Long id) {
        return Result.success(performanceAppService.getTaskById(id));
    }

    @Operation(summary = "创建考核任务")
    @PostMapping("/tasks")
    @RequirePermission("hr:performance:create")
    @OperationLog(module = "绩效考核", action = "创建考核任务")
    public Result<PerformanceTaskDTO> createTask(@RequestBody CreatePerformanceTaskCommand command) {
        return Result.success(performanceAppService.createTask(command));
    }

    @Operation(summary = "启动考核任务")
    @PostMapping("/tasks/{id}/start")
    @RequirePermission("hr:performance:edit")
    @OperationLog(module = "绩效考核", action = "启动考核任务")
    public Result<Void> startTask(@PathVariable Long id) {
        performanceAppService.startTask(id);
        return Result.success();
    }

    @Operation(summary = "完成考核任务")
    @PostMapping("/tasks/{id}/complete")
    @RequirePermission("hr:performance:edit")
    @OperationLog(module = "绩效考核", action = "完成考核任务")
    public Result<Void> completeTask(@PathVariable Long id) {
        performanceAppService.completeTask(id);
        return Result.success();
    }

    @Operation(summary = "获取考核任务统计")
    @GetMapping("/tasks/{id}/statistics")
    @RequirePermission("hr:performance:detail")
    public Result<Map<String, Object>> getTaskStatistics(@PathVariable Long id) {
        return Result.success(performanceAppService.getTaskStatistics(id));
    }


    // ==================== 考核指标 ====================

    @Operation(summary = "查询考核指标列表")
    @GetMapping("/indicators")
    @RequirePermission("hr:performance:list")
    public Result<List<PerformanceIndicatorDTO>> listIndicators(
            @Parameter(description = "分类") @RequestParam(required = false) String category,
            @Parameter(description = "适用角色") @RequestParam(required = false) String applicableRole) {
        return Result.success(performanceAppService.listIndicators(category, applicableRole));
    }

    @Operation(summary = "创建考核指标")
    @PostMapping("/indicators")
    @RequirePermission("hr:performance:create")
    @OperationLog(module = "绩效考核", action = "创建考核指标")
    public Result<PerformanceIndicatorDTO> createIndicator(@RequestBody CreateIndicatorCommand command) {
        return Result.success(performanceAppService.createIndicator(command));
    }

    @Operation(summary = "更新考核指标")
    @PutMapping("/indicators/{id}")
    @RequirePermission("hr:performance:edit")
    @OperationLog(module = "绩效考核", action = "更新考核指标")
    public Result<PerformanceIndicatorDTO> updateIndicator(
            @PathVariable Long id, @RequestBody CreateIndicatorCommand command) {
        return Result.success(performanceAppService.updateIndicator(id, command));
    }

    @Operation(summary = "删除考核指标")
    @DeleteMapping("/indicators/{id}")
    @RequirePermission("hr:performance:delete")
    @OperationLog(module = "绩效考核", action = "删除考核指标")
    public Result<Void> deleteIndicator(@PathVariable Long id) {
        performanceAppService.deleteIndicator(id);
        return Result.success();
    }

    // ==================== 绩效评价 ====================

    @Operation(summary = "提交绩效评价")
    @PostMapping("/evaluations")
    @OperationLog(module = "绩效考核", action = "提交绩效评价")
    public Result<PerformanceEvaluationDTO> submitEvaluation(@RequestBody SubmitEvaluationCommand command) {
        return Result.success(performanceAppService.submitEvaluation(command));
    }

    @Operation(summary = "获取员工的评价记录")
    @GetMapping("/evaluations")
    public Result<List<PerformanceEvaluationDTO>> getEmployeeEvaluations(
            @Parameter(description = "考核任务ID") @RequestParam Long taskId,
            @Parameter(description = "员工ID") @RequestParam Long employeeId) {
        return Result.success(performanceAppService.getEmployeeEvaluations(taskId, employeeId));
    }

    @Operation(summary = "获取我待评价的记录")
    @GetMapping("/evaluations/pending")
    public Result<List<PerformanceEvaluationDTO>> getMyPendingEvaluations() {
        return Result.success(performanceAppService.getMyPendingEvaluations());
    }

    @Operation(summary = "获取评价详情")
    @GetMapping("/evaluations/{id}")
    public Result<PerformanceEvaluationDTO> getEvaluationDetail(@PathVariable Long id) {
        return Result.success(performanceAppService.getEvaluationDetail(id));
    }
}
