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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 绩效考核接口 */
@Tag(name = "绩效考核", description = "绩效考核管理相关接口")
@RestController
@RequestMapping("/hr/performance")
@RequiredArgsConstructor
public class PerformanceController {

  /** 绩效服务. */
  private final PerformanceAppService performanceAppService;

  // ==================== 考核任务 ====================

  /**
   * 分页查询考核任务
   *
   * @param query 分页查询条件
   * @param year 年份
   * @param periodType 周期类型
   * @param status 状态
   * @return 分页结果
   */
  @Operation(summary = "分页查询考核任务")
  @GetMapping("/tasks")
  @RequirePermission("hr:performance:list")
  public Result<PageResult<PerformanceTaskDTO>> listTasks(
      final PageQuery query,
      @Parameter(description = "年份") @RequestParam(required = false) final Integer year,
      @Parameter(description = "周期类型") @RequestParam(required = false) final String periodType,
      @Parameter(description = "状态") @RequestParam(required = false) final String status) {
    return Result.success(performanceAppService.listTasks(query, year, periodType, status));
  }

  /**
   * 获取考核任务详情
   *
   * @param id 任务ID
   * @return 任务详情
   */
  @Operation(summary = "获取考核任务详情")
  @GetMapping("/tasks/{id}")
  @RequirePermission("hr:performance:detail")
  public Result<PerformanceTaskDTO> getTaskById(@PathVariable final Long id) {
    return Result.success(performanceAppService.getTaskById(id));
  }

  /**
   * 创建考核任务
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建考核任务")
  @PostMapping("/tasks")
  @RequirePermission("hr:performance:create")
  @OperationLog(module = "绩效考核", action = "创建考核任务")
  public Result<PerformanceTaskDTO> createTask(
      @RequestBody final CreatePerformanceTaskCommand command) {
    return Result.success(performanceAppService.createTask(command));
  }

  /**
   * 启动考核任务
   *
   * @param id 任务ID
   * @return 无返回
   */
  @Operation(summary = "启动考核任务")
  @PostMapping("/tasks/{id}/start")
  @RequirePermission("hr:performance:update")
  @OperationLog(module = "绩效考核", action = "启动考核任务")
  public Result<Void> startTask(@PathVariable final Long id) {
    performanceAppService.startTask(id);
    return Result.success();
  }

  /**
   * 完成考核任务
   *
   * @param id 任务ID
   * @return 无返回
   */
  @Operation(summary = "完成考核任务")
  @PostMapping("/tasks/{id}/complete")
  @RequirePermission("hr:performance:update")
  @OperationLog(module = "绩效考核", action = "完成考核任务")
  public Result<Void> completeTask(@PathVariable final Long id) {
    performanceAppService.completeTask(id);
    return Result.success();
  }

  /**
   * 获取考核任务统计
   *
   * @param id 任务ID
   * @return 统计结果
   */
  @Operation(summary = "获取考核任务统计")
  @GetMapping("/tasks/{id}/statistics")
  @RequirePermission("hr:performance:detail")
  public Result<Map<String, Object>> getTaskStatistics(@PathVariable final Long id) {
    return Result.success(performanceAppService.getTaskStatistics(id));
  }

  // ==================== 考核指标 ====================

  /**
   * 查询考核指标列表
   *
   * @param category 分类
   * @param applicableRole 适用角色
   * @return 指标列表
   */
  @Operation(summary = "查询考核指标列表")
  @GetMapping("/indicators")
  @RequirePermission("hr:performance:list")
  public Result<List<PerformanceIndicatorDTO>> listIndicators(
      @Parameter(description = "分类") @RequestParam(required = false) final String category,
      @Parameter(description = "适用角色") @RequestParam(required = false)
          final String applicableRole) {
    return Result.success(performanceAppService.listIndicators(category, applicableRole));
  }

  /**
   * 创建考核指标
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建考核指标")
  @PostMapping("/indicators")
  @RequirePermission("hr:performance:create")
  @OperationLog(module = "绩效考核", action = "创建考核指标")
  public Result<PerformanceIndicatorDTO> createIndicator(
      @RequestBody final CreateIndicatorCommand command) {
    return Result.success(performanceAppService.createIndicator(command));
  }

  /**
   * 更新考核指标
   *
   * @param id 指标ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新考核指标")
  @PutMapping("/indicators/{id}")
  @RequirePermission("hr:performance:update")
  @OperationLog(module = "绩效考核", action = "更新考核指标")
  public Result<PerformanceIndicatorDTO> updateIndicator(
      @PathVariable final Long id, @RequestBody final CreateIndicatorCommand command) {
    return Result.success(performanceAppService.updateIndicator(id, command));
  }

  /**
   * 删除考核指标
   *
   * @param id 指标ID
   * @return 无返回
   */
  @Operation(summary = "删除考核指标")
  @DeleteMapping("/indicators/{id}")
  @RequirePermission("hr:performance:delete")
  @OperationLog(module = "绩效考核", action = "删除考核指标")
  public Result<Void> deleteIndicator(@PathVariable final Long id) {
    performanceAppService.deleteIndicator(id);
    return Result.success();
  }

  // ==================== 绩效评价 ====================

  /**
   * 提交绩效评价
   *
   * @param command 提交命令
   * @return 评价结果
   */
  @Operation(summary = "提交绩效评价")
  @PostMapping("/evaluations")
  @OperationLog(module = "绩效考核", action = "提交绩效评价")
  public Result<PerformanceEvaluationDTO> submitEvaluation(
      @RequestBody final SubmitEvaluationCommand command) {
    return Result.success(performanceAppService.submitEvaluation(command));
  }

  /**
   * 获取员工的评价记录
   *
   * @param taskId 任务ID
   * @param employeeId 员工ID
   * @return 评价记录列表
   */
  @Operation(summary = "获取员工的评价记录")
  @GetMapping("/evaluations")
  public Result<List<PerformanceEvaluationDTO>> getEmployeeEvaluations(
      @Parameter(description = "考核任务ID") @RequestParam final Long taskId,
      @Parameter(description = "员工ID") @RequestParam final Long employeeId) {
    return Result.success(performanceAppService.getEmployeeEvaluations(taskId, employeeId));
  }

  /**
   * 获取我待评价的记录
   *
   * @return 待评价记录列表
   */
  @Operation(summary = "获取我待评价的记录")
  @GetMapping("/evaluations/pending")
  public Result<List<PerformanceEvaluationDTO>> getMyPendingEvaluations() {
    return Result.success(performanceAppService.getMyPendingEvaluations());
  }

  /**
   * 获取评价详情
   *
   * @param id 评价ID
   * @return 评价详情
   */
  @Operation(summary = "获取评价详情")
  @GetMapping("/evaluations/{id}")
  public Result<PerformanceEvaluationDTO> getEvaluationDetail(@PathVariable final Long id) {
    return Result.success(performanceAppService.getEvaluationDetail(id));
  }
}
