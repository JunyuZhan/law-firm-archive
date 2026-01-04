package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateTimesheetCommand;
import com.lawfirm.application.matter.dto.TimesheetDTO;
import com.lawfirm.application.matter.dto.TimesheetQueryDTO;
import com.lawfirm.application.matter.dto.TimesheetSummaryDTO;
import com.lawfirm.application.matter.service.TimesheetAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 工时管理接口
 */
@RestController
@RequestMapping("/timesheets")
@RequiredArgsConstructor
public class TimesheetController {

    private final TimesheetAppService timesheetAppService;

    /**
     * 分页查询工时
     */
    @GetMapping
    @RequirePermission("timesheet:list")
    public Result<PageResult<TimesheetDTO>> list(TimesheetQueryDTO query) {
        return Result.success(timesheetAppService.listTimesheets(query));
    }

    /**
     * 获取工时详情
     */
    @GetMapping("/{id}")
    @RequirePermission("timesheet:view")
    public Result<TimesheetDTO> getById(@PathVariable Long id) {
        return Result.success(timesheetAppService.getTimesheetById(id));
    }

    /**
     * 创建工时记录
     */
    @PostMapping
    @RequirePermission("timesheet:record")
    @OperationLog(module = "工时管理", action = "记录工时")
    public Result<TimesheetDTO> create(@Valid @RequestBody CreateTimesheetCommand command) {
        return Result.success(timesheetAppService.createTimesheet(command));
    }

    /**
     * 更新工时记录
     */
    @PutMapping("/{id}")
    @RequirePermission("timesheet:record")
    @OperationLog(module = "工时管理", action = "更新工时")
    public Result<TimesheetDTO> update(@PathVariable Long id,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate,
                                       @RequestParam(required = false) BigDecimal hours,
                                       @RequestParam(required = false) String workType,
                                       @RequestParam(required = false) String workContent,
                                       @RequestParam(required = false) Boolean billable) {
        return Result.success(timesheetAppService.updateTimesheet(id, workDate, hours, workType, workContent, billable));
    }

    /**
     * 删除工时记录
     */
    @DeleteMapping("/{id}")
    @RequirePermission("timesheet:record")
    @OperationLog(module = "工时管理", action = "删除工时")
    public Result<Void> delete(@PathVariable Long id) {
        timesheetAppService.deleteTimesheet(id);
        return Result.success();
    }

    /**
     * 提交工时
     */
    @PostMapping("/{id}/submit")
    @RequirePermission("timesheet:record")
    @OperationLog(module = "工时管理", action = "提交工时")
    public Result<TimesheetDTO> submit(@PathVariable Long id) {
        return Result.success(timesheetAppService.submitTimesheet(id));
    }

    /**
     * 批量提交工时
     */
    @PostMapping("/batch-submit")
    @RequirePermission("timesheet:record")
    @OperationLog(module = "工时管理", action = "批量提交工时")
    public Result<Void> batchSubmit(@RequestBody List<Long> ids) {
        timesheetAppService.batchSubmit(ids);
        return Result.success();
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("timesheet:approve")
    @OperationLog(module = "工时管理", action = "审批通过")
    public Result<TimesheetDTO> approve(@PathVariable Long id,
                                        @RequestParam(required = false) String comment) {
        return Result.success(timesheetAppService.approveTimesheet(id, comment));
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("timesheet:approve")
    @OperationLog(module = "工时管理", action = "审批拒绝")
    public Result<TimesheetDTO> reject(@PathVariable Long id,
                                       @RequestParam(required = false) String comment) {
        return Result.success(timesheetAppService.rejectTimesheet(id, comment));
    }

    /**
     * 获取待审批列表
     */
    @GetMapping("/pending")
    @RequirePermission("timesheet:approve")
    public Result<List<TimesheetDTO>> getPending() {
        return Result.success(timesheetAppService.getPendingApproval());
    }

    /**
     * 获取我的工时
     */
    @GetMapping("/my")
    @RequirePermission("timesheet:record")
    public Result<List<TimesheetDTO>> getMyTimesheets(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(timesheetAppService.getMyTimesheets(startDate, endDate));
    }

    /**
     * 获取用户月度汇总
     */
    @GetMapping("/summary/user/{userId}")
    @RequirePermission("timesheet:list")
    public Result<TimesheetSummaryDTO> getUserSummary(@PathVariable Long userId,
                                                      @RequestParam int year,
                                                      @RequestParam int month) {
        return Result.success(timesheetAppService.getUserMonthlySummary(userId, year, month));
    }

    /**
     * 获取案件工时汇总
     */
    @GetMapping("/summary/matter/{matterId}")
    @RequirePermission("timesheet:list")
    public Result<TimesheetSummaryDTO> getMatterSummary(@PathVariable Long matterId) {
        return Result.success(timesheetAppService.getMatterSummary(matterId));
    }
}
