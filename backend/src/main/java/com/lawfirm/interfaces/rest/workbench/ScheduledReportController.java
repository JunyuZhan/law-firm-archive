package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.command.CreateScheduledReportCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ScheduledReportDTO;
import com.lawfirm.application.workbench.dto.ScheduledReportLogDTO;
import com.lawfirm.application.workbench.service.ScheduledReportAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/** 定时报表控制器 */
@Tag(name = "定时报表", description = "定时报表任务管理接口")
@RestController
@RequestMapping("/workbench/scheduled-report")
@RequiredArgsConstructor
public class ScheduledReportController {

  /** 定时报表应用服务. */
  private final ScheduledReportAppService scheduledReportAppService;

  /**
   * 分页查询定时报表任务
   *
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @param keyword 关键词
   * @param status 状态
   * @return 分页结果
   */
  @Operation(summary = "分页查询定时报表任务")
  @GetMapping
  @RequirePermission("report:scheduled:list")
  public Result<PageResult<ScheduledReportDTO>> list(
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") final int pageSize,
      @Parameter(description = "关键词") @RequestParam(required = false) final String keyword,
      @Parameter(description = "状态") @RequestParam(required = false) final String status) {
    PageResult<ScheduledReportDTO> result =
        scheduledReportAppService.listScheduledReports(pageNum, pageSize, keyword, status);
    return Result.success(result);
  }

  /**
   * 获取定时任务详情
   *
   * @param id 任务ID
   * @return 任务详情
   */
  @Operation(summary = "获取定时任务详情")
  @GetMapping("/{id}")
  @RequirePermission("report:scheduled:view")
  public Result<ScheduledReportDTO> getById(@PathVariable final Long id) {
    ScheduledReportDTO task = scheduledReportAppService.getScheduledReportById(id);
    return Result.success(task);
  }

  /**
   * 创建定时报表任务
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @Operation(summary = "创建定时报表任务")
  @PostMapping
  @RequirePermission("report:scheduled:create")
  @OperationLog(module = "定时报表", action = "创建任务")
  public Result<ScheduledReportDTO> create(
      @RequestBody @Valid final CreateScheduledReportCommand command) {
    ScheduledReportDTO task = scheduledReportAppService.createScheduledReport(command);
    return Result.success(task);
  }

  /**
   * 更新定时报表任务
   *
   * @param id 任务ID
   * @param command 更新命令
   * @return 更新结果
   */
  @Operation(summary = "更新定时报表任务")
  @PutMapping("/{id}")
  @RequirePermission("report:scheduled:update")
  @OperationLog(module = "定时报表", action = "更新任务")
  public Result<ScheduledReportDTO> update(
      @PathVariable final Long id, @RequestBody @Valid final CreateScheduledReportCommand command) {
    ScheduledReportDTO task = scheduledReportAppService.updateScheduledReport(id, command);
    return Result.success(task);
  }

  /**
   * 删除定时报表任务
   *
   * @param id 任务ID
   * @return 操作结果
   */
  @Operation(summary = "删除定时报表任务")
  @DeleteMapping("/{id}")
  @RequirePermission("report:scheduled:delete")
  @OperationLog(module = "定时报表", action = "删除任务")
  public Result<Void> delete(@PathVariable final Long id) {
    scheduledReportAppService.deleteScheduledReport(id);
    return Result.success();
  }

  /**
   * 启用任务
   *
   * @param id 任务ID
   * @return 操作结果
   */
  @Operation(summary = "启用任务")
  @PostMapping("/{id}/enable")
  @RequirePermission("report:scheduled:update")
  @OperationLog(module = "定时报表", action = "启用任务")
  public Result<Void> enable(@PathVariable final Long id) {
    scheduledReportAppService.changeTaskStatus(id, "ACTIVE");
    return Result.success();
  }

  /**
   * 暂停任务
   *
   * @param id 任务ID
   * @return 操作结果
   */
  @Operation(summary = "暂停任务")
  @PostMapping("/{id}/pause")
  @RequirePermission("report:scheduled:update")
  @OperationLog(module = "定时报表", action = "暂停任务")
  public Result<Void> pause(@PathVariable final Long id) {
    scheduledReportAppService.changeTaskStatus(id, "PAUSED");
    return Result.success();
  }

  /**
   * 立即执行任务
   *
   * @param id 任务ID
   * @return 执行结果
   */
  @Operation(summary = "立即执行任务")
  @PostMapping("/{id}/execute")
  @RequirePermission("report:scheduled:execute")
  @OperationLog(module = "定时报表", action = "立即执行")
  public Result<ReportDTO> executeNow(@PathVariable final Long id) {
    ReportDTO report = scheduledReportAppService.executeNow(id);
    return Result.success(report);
  }

  /**
   * 查询执行记录
   *
   * @param id 任务ID
   * @param pageNum 页码
   * @param pageSize 每页数量
   * @param status 状态
   * @return 执行记录列表
   */
  @Operation(summary = "查询执行记录")
  @GetMapping("/{id}/logs")
  @RequirePermission("report:scheduled:view")
  public Result<PageResult<ScheduledReportLogDTO>> listLogs(
      @PathVariable final Long id,
      @Parameter(description = "页码") @RequestParam(defaultValue = "1") final int pageNum,
      @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") final int pageSize,
      @Parameter(description = "状态") @RequestParam(required = false) final String status) {
    PageResult<ScheduledReportLogDTO> result =
        scheduledReportAppService.listExecuteLogs(id, pageNum, pageSize, status);
    return Result.success(result);
  }
}
