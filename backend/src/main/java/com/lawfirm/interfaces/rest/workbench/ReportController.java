package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.AvailableReportDTO;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportQueryDTO;
import com.lawfirm.application.workbench.service.ReportAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RateLimit;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 报表中心 Controller */
@Tag(name = "报表中心", description = "报表生成和下载相关接口")
@RestController
@RequestMapping("/workbench/report")
@RequiredArgsConstructor
public class ReportController {

  /** 报表应用服务. */
  private final ReportAppService reportAppService;

  /**
   * 获取可用报表列表
   *
   * @return 可用报表列表
   */
  @Operation(summary = "获取可用报表列表")
  @GetMapping("/available")
  @RequirePermission("report:list")
  public Result<List<AvailableReportDTO>> getAvailableReports() {
    return Result.success(reportAppService.getAvailableReports());
  }

  /**
   * 分页查询报表记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询报表记录")
  @GetMapping
  @RequirePermission("report:list")
  public Result<PageResult<ReportDTO>> listReports(final ReportQueryDTO query) {
    return Result.success(reportAppService.listReports(query));
  }

  /**
   * 根据ID查询报表
   *
   * @param id 报表ID
   * @return 报表详情
   */
  @Operation(summary = "根据ID查询报表")
  @GetMapping("/{id}")
  @RequirePermission("report:detail")
  public Result<ReportDTO> getReport(@PathVariable final Long id) {
    return Result.success(reportAppService.getReportById(id));
  }

  /**
   * 同步生成报表（小型报表）
   *
   * @param command 生成命令
   * @return 生成的报表
   */
  @Operation(summary = "同步生成报表（小型报表）")
  @PostMapping("/generate")
  @RequirePermission("report:generate")
  @OperationLog(module = "报表中心", action = "生成报表")
  @RateLimit(key = "report:generate", limit = 10, period = 60, message = "报表生成请求过于频繁，请稍后再试")
  public Result<ReportDTO> generateReport(@RequestBody @Valid final GenerateReportCommand command) {
    return Result.success(reportAppService.generateReport(command));
  }

  /**
   * 异步提交报表生成任务（大型报表推荐）
   *
   * @param command 生成命令
   * @return 生成任务详情
   */
  @Operation(summary = "异步提交报表生成任务（大型报表推荐）", description = "立即返回报表记录，后台异步生成。通过 /status/{id} 轮询状态")
  @PostMapping("/submit")
  @RequirePermission("report:generate")
  @OperationLog(module = "报表中心", action = "提交报表生成任务")
  @RateLimit(key = "report:submit", limit = 5, period = 60, message = "报表生成请求过于频繁，请稍后再试")
  public Result<ReportDTO> submitReportGeneration(
      @RequestBody @Valid final GenerateReportCommand command) {
    return Result.success(reportAppService.submitReportGeneration(command));
  }

  /**
   * 查询报表生成状态
   *
   * @param id 报表ID
   * @return 报表状态
   */
  @Operation(summary = "查询报表生成状态", description = "用于轮询异步报表的生成状态")
  @GetMapping("/status/{id}")
  @RequirePermission("report:detail")
  public Result<ReportDTO> getReportStatus(@PathVariable final Long id) {
    return Result.success(reportAppService.getReportStatus(id));
  }

  /**
   * 获取报表下载URL
   *
   * @param id 报表ID
   * @return 下载URL
   */
  @Operation(summary = "获取报表下载URL")
  @GetMapping("/{id}/download-url")
  @RequirePermission("report:download")
  public Result<String> getReportDownloadUrl(@PathVariable final Long id) {
    try {
      String downloadUrl = reportAppService.getReportDownloadUrl(id);
      return Result.success(downloadUrl);
    } catch (Exception e) {
      return Result.error("获取下载链接失败: " + e.getMessage());
    }
  }

  /**
   * 删除报表
   *
   * @param id 报表ID
   * @return 操作结果
   */
  @Operation(summary = "删除报表")
  @DeleteMapping("/{id}")
  @RequirePermission("report:delete")
  @OperationLog(module = "报表中心", action = "删除报表")
  public Result<Void> deleteReport(@PathVariable final Long id) {
    reportAppService.deleteReport(id);
    return Result.success();
  }
}
