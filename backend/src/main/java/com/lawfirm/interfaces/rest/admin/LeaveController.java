package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.ApplyLeaveCommand;
import com.lawfirm.application.admin.command.ApproveLeaveCommand;
import com.lawfirm.application.admin.dto.*;
import com.lawfirm.application.admin.service.LeaveAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 请假管理接口
 */
@Tag(name = "请假管理", description = "请假管理相关接口")
@RestController
@RequestMapping("/admin/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveAppService leaveAppService;

    @Operation(summary = "获取请假类型列表")
    @GetMapping("/types")
    public Result<List<LeaveTypeDTO>> listTypes() {
        return Result.success(leaveAppService.listLeaveTypes());
    }

    @Operation(summary = "分页查询请假申请")
    @GetMapping("/applications")
    @RequirePermission("admin:leave:list")
    public Result<PageResult<LeaveApplicationDTO>> listApplications(LeaveApplicationQueryDTO query) {
        return Result.success(leaveAppService.listApplications(query));
    }

    @Operation(summary = "提交请假申请")
    @PostMapping("/applications")
    @OperationLog(module = "请假管理", action = "提交请假申请")
    public Result<LeaveApplicationDTO> applyLeave(@RequestBody ApplyLeaveCommand command) {
        return Result.success(leaveAppService.applyLeave(command));
    }

    @Operation(summary = "审批请假申请")
    @PostMapping("/applications/approve")
    @RequirePermission("admin:leave:approve")
    @OperationLog(module = "请假管理", action = "审批请假申请")
    public Result<LeaveApplicationDTO> approveLeave(@RequestBody ApproveLeaveCommand command) {
        return Result.success(leaveAppService.approveLeave(command));
    }

    @Operation(summary = "取消请假申请")
    @PostMapping("/applications/{id}/cancel")
    @OperationLog(module = "请假管理", action = "取消请假申请")
    public Result<Void> cancelApplication(@PathVariable Long id) {
        leaveAppService.cancelApplication(id);
        return Result.success();
    }

    @Operation(summary = "获取待审批列表")
    @GetMapping("/applications/pending")
    @RequirePermission("admin:leave:approve")
    public Result<List<LeaveApplicationDTO>> getPendingApplications() {
        return Result.success(leaveAppService.getPendingApplications());
    }

    @Operation(summary = "获取假期余额")
    @GetMapping("/balance")
    public Result<List<LeaveBalanceDTO>> getUserBalance(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer year) {
        return Result.success(leaveAppService.getUserBalance(userId, year));
    }

    @Operation(summary = "初始化用户年度假期余额")
    @PostMapping("/balance/init")
    @RequirePermission("admin:leave:manage")
    @OperationLog(module = "请假管理", action = "初始化用户年度假期余额")
    public Result<Void> initUserBalance(@RequestParam Long userId, @RequestParam Integer year) {
        leaveAppService.initUserBalance(userId, year);
        return Result.success();
    }
}
