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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表中心 Controller
 */
@Tag(name = "报表中心", description = "报表生成和下载相关接口")
@RestController
@RequestMapping("/workbench/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportAppService reportAppService;

    @Operation(summary = "获取可用报表列表")
    @GetMapping("/available")
    @RequirePermission("report:list")
    public Result<List<AvailableReportDTO>> getAvailableReports() {
        return Result.success(reportAppService.getAvailableReports());
    }

    @Operation(summary = "分页查询报表记录")
    @GetMapping
    @RequirePermission("report:list")
    public Result<PageResult<ReportDTO>> listReports(ReportQueryDTO query) {
        return Result.success(reportAppService.listReports(query));
    }

    @Operation(summary = "根据ID查询报表")
    @GetMapping("/{id}")
    @RequirePermission("report:detail")
    public Result<ReportDTO> getReport(@PathVariable Long id) {
        return Result.success(reportAppService.getReportById(id));
    }

    @Operation(summary = "同步生成报表（小型报表）")
    @PostMapping("/generate")
    @RequirePermission("report:generate")
    @OperationLog(module = "报表中心", action = "生成报表")
    @RateLimit(key = "report:generate", limit = 10, period = 60, message = "报表生成请求过于频繁，请稍后再试")
    public Result<ReportDTO> generateReport(@RequestBody @Valid GenerateReportCommand command) {
        return Result.success(reportAppService.generateReport(command));
    }

    @Operation(summary = "异步提交报表生成任务（大型报表推荐）", description = "立即返回报表记录，后台异步生成。通过 /status/{id} 轮询状态")
    @PostMapping("/submit")
    @RequirePermission("report:generate")
    @OperationLog(module = "报表中心", action = "提交报表生成任务")
    @RateLimit(key = "report:submit", limit = 5, period = 60, message = "报表生成请求过于频繁，请稍后再试")
    public Result<ReportDTO> submitReportGeneration(@RequestBody @Valid GenerateReportCommand command) {
        return Result.success(reportAppService.submitReportGeneration(command));
    }

    @Operation(summary = "查询报表生成状态", description = "用于轮询异步报表的生成状态")
    @GetMapping("/status/{id}")
    @RequirePermission("report:detail")
    public Result<ReportDTO> getReportStatus(@PathVariable Long id) {
        return Result.success(reportAppService.getReportStatus(id));
    }

    @Operation(summary = "获取报表下载URL")
    @GetMapping("/{id}/download-url")
    @RequirePermission("report:download")
    public Result<String> getReportDownloadUrl(@PathVariable Long id) {
        try {
            String downloadUrl = reportAppService.getReportDownloadUrl(id);
            return Result.success(downloadUrl);
        } catch (Exception e) {
            return Result.error("获取下载链接失败: " + e.getMessage());
        }
    }

    @Operation(summary = "删除报表")
    @DeleteMapping("/{id}")
    @RequirePermission("report:delete")
    @OperationLog(module = "报表中心", action = "删除报表")
    public Result<Void> deleteReport(@PathVariable Long id) {
        reportAppService.deleteReport(id);
        return Result.success();
    }
}

