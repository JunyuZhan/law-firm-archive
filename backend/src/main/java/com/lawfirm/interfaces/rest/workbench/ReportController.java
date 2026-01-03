package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.command.GenerateReportCommand;
import com.lawfirm.application.workbench.dto.ReportDTO;
import com.lawfirm.application.workbench.dto.ReportQueryDTO;
import com.lawfirm.application.workbench.service.ReportAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public Result<List<Map<String, Object>>> getAvailableReports() {
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

    @Operation(summary = "生成报表")
    @PostMapping("/generate")
    @RequirePermission("report:generate")
    @OperationLog(module = "报表中心", action = "生成报表")
    public Result<ReportDTO> generateReport(@RequestBody @Valid GenerateReportCommand command) {
        return Result.success(reportAppService.generateReport(command));
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

