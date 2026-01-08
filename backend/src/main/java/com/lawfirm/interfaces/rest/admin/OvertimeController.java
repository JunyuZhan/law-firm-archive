package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.ApplyOvertimeCommand;
import com.lawfirm.application.admin.dto.OvertimeApplicationDTO;
import com.lawfirm.application.admin.service.OvertimeAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 加班申请接口（M8-004）
 */
@Tag(name = "加班管理", description = "加班申请和审批")
@RestController
@RequestMapping("/admin/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeAppService overtimeAppService;

    @Operation(summary = "申请加班")
    @PostMapping("/apply")
    @RequirePermission("admin:overtime:apply")
    @OperationLog(module = "加班管理", action = "申请加班")
    public Result<OvertimeApplicationDTO> applyOvertime(@RequestBody @Valid ApplyOvertimeCommand command) {
        return Result.success(overtimeAppService.applyOvertime(command));
    }

    @Operation(summary = "审批加班申请")
    @PostMapping("/{id}/approve")
    @RequirePermission("admin:overtime:approve")
    @OperationLog(module = "加班管理", action = "审批加班申请")
    public Result<OvertimeApplicationDTO> approveOvertime(@PathVariable Long id, @RequestBody ApproveRequest request) {
        return Result.success(overtimeAppService.approveOvertime(id, request.getApproved(), request.getComment()));
    }

    @Operation(summary = "查询我的加班申请")
    @GetMapping("/my")
    @RequirePermission("admin:overtime:list")
    public Result<List<OvertimeApplicationDTO>> getMyApplications() {
        return Result.success(overtimeAppService.getMyApplications());
    }

    @Operation(summary = "查询指定日期范围的加班申请")
    @GetMapping("/range")
    @RequirePermission("admin:overtime:list")
    public Result<List<OvertimeApplicationDTO>> getApplicationsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return Result.success(overtimeAppService.getApplicationsByDateRange(startDate, endDate));
    }

    @Data
    public static class ApproveRequest {
        private Boolean approved;
        private String comment;
    }
}

