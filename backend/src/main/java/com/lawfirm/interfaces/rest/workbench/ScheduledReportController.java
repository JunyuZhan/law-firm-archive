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
import org.springframework.web.bind.annotation.*;

/**
 * 定时报表控制器
 */
@Tag(name = "定时报表", description = "定时报表任务管理接口")
@RestController
@RequestMapping("/api/scheduled-reports")
@RequiredArgsConstructor
public class ScheduledReportController {

    private final ScheduledReportAppService scheduledReportAppService;

    @Operation(summary = "分页查询定时报表任务")
    @GetMapping
    @RequirePermission("report:scheduled:list")
    public Result<PageResult<ScheduledReportDTO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        PageResult<ScheduledReportDTO> result = scheduledReportAppService.listScheduledReports(
                pageNum, pageSize, keyword, status);
        return Result.success(result);
    }

    @Operation(summary = "获取定时任务详情")
    @GetMapping("/{id}")
    @RequirePermission("report:scheduled:view")
    public Result<ScheduledReportDTO> getById(@PathVariable Long id) {
        ScheduledReportDTO task = scheduledReportAppService.getScheduledReportById(id);
        return Result.success(task);
    }

    @Operation(summary = "创建定时报表任务")
    @PostMapping
    @RequirePermission("report:scheduled:create")
    @OperationLog(module = "定时报表", action = "创建任务")
    public Result<ScheduledReportDTO> create(@RequestBody @Valid CreateScheduledReportCommand command) {
        ScheduledReportDTO task = scheduledReportAppService.createScheduledReport(command);
        return Result.success(task);
    }

    @Operation(summary = "更新定时报表任务")
    @PutMapping("/{id}")
    @RequirePermission("report:scheduled:edit")
    @OperationLog(module = "定时报表", action = "更新任务")
    public Result<ScheduledReportDTO> update(@PathVariable Long id,
                                              @RequestBody @Valid CreateScheduledReportCommand command) {
        ScheduledReportDTO task = scheduledReportAppService.updateScheduledReport(id, command);
        return Result.success(task);
    }

    @Operation(summary = "删除定时报表任务")
    @DeleteMapping("/{id}")
    @RequirePermission("report:scheduled:delete")
    @OperationLog(module = "定时报表", action = "删除任务")
    public Result<Void> delete(@PathVariable Long id) {
        scheduledReportAppService.deleteScheduledReport(id);
        return Result.success();
    }

    @Operation(summary = "启用任务")
    @PostMapping("/{id}/enable")
    @RequirePermission("report:scheduled:edit")
    @OperationLog(module = "定时报表", action = "启用任务")
    public Result<Void> enable(@PathVariable Long id) {
        scheduledReportAppService.changeTaskStatus(id, "ACTIVE");
        return Result.success();
    }

    @Operation(summary = "暂停任务")
    @PostMapping("/{id}/pause")
    @RequirePermission("report:scheduled:edit")
    @OperationLog(module = "定时报表", action = "暂停任务")
    public Result<Void> pause(@PathVariable Long id) {
        scheduledReportAppService.changeTaskStatus(id, "PAUSED");
        return Result.success();
    }

    @Operation(summary = "立即执行任务")
    @PostMapping("/{id}/execute")
    @RequirePermission("report:scheduled:execute")
    @OperationLog(module = "定时报表", action = "立即执行")
    public Result<ReportDTO> executeNow(@PathVariable Long id) {
        ReportDTO report = scheduledReportAppService.executeNow(id);
        return Result.success(report);
    }

    @Operation(summary = "查询执行记录")
    @GetMapping("/{id}/logs")
    @RequirePermission("report:scheduled:view")
    public Result<PageResult<ScheduledReportLogDTO>> listLogs(
            @PathVariable Long id,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        PageResult<ScheduledReportLogDTO> result = scheduledReportAppService.listExecuteLogs(
                id, pageNum, pageSize, status);
        return Result.success(result);
    }
}
