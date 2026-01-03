package com.lawfirm.interfaces.rest.client;

import com.lawfirm.application.client.command.CreateConflictCheckCommand;
import com.lawfirm.application.client.dto.ConflictCheckDTO;
import com.lawfirm.application.client.dto.ConflictCheckQueryDTO;
import com.lawfirm.application.client.service.ConflictCheckAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 利冲检查 Controller
 */
@RestController
@RequestMapping("/client/conflict-check")
@RequiredArgsConstructor
public class ConflictCheckController {

    private final ConflictCheckAppService conflictCheckAppService;

    /**
     * 分页查询利冲检查列表
     */
    @GetMapping("/list")
    @RequirePermission("conflict:list")
    public Result<PageResult<ConflictCheckDTO>> listConflictChecks(ConflictCheckQueryDTO query) {
        PageResult<ConflictCheckDTO> result = conflictCheckAppService.listConflictChecks(query);
        return Result.success(result);
    }

    /**
     * 获取利冲检查详情
     */
    @GetMapping("/{id}")
    @RequirePermission("conflict:list")
    public Result<ConflictCheckDTO> getConflictCheck(@PathVariable Long id) {
        ConflictCheckDTO check = conflictCheckAppService.getConflictCheckById(id);
        return Result.success(check);
    }

    /**
     * 创建利冲检查
     */
    @PostMapping
    @RequirePermission("conflict:create")
    @OperationLog(module = "利冲检查", action = "创建利冲检查")
    public Result<ConflictCheckDTO> createConflictCheck(@RequestBody @Valid CreateConflictCheckCommand command) {
        ConflictCheckDTO check = conflictCheckAppService.createConflictCheck(command);
        return Result.success(check);
    }

    /**
     * 审核通过（豁免）
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("conflict:approve")
    @OperationLog(module = "利冲检查", action = "审核通过")
    public Result<Void> approve(@PathVariable Long id, @RequestBody ReviewRequest request) {
        conflictCheckAppService.approve(id, request.getComment());
        return Result.success();
    }

    /**
     * 审核拒绝
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("conflict:approve")
    @OperationLog(module = "利冲检查", action = "审核拒绝")
    public Result<Void> reject(@PathVariable Long id, @RequestBody ReviewRequest request) {
        conflictCheckAppService.reject(id, request.getComment());
        return Result.success();
    }

    /**
     * 申请利益冲突豁免
     */
    @PostMapping("/exemption/apply")
    @RequirePermission("conflict:exemption")
    @OperationLog(module = "利冲检查", action = "申请豁免")
    public Result<ConflictCheckDTO> applyExemption(@RequestBody @Valid com.lawfirm.application.client.command.ApplyExemptionCommand command) {
        ConflictCheckDTO result = conflictCheckAppService.applyExemption(command);
        return Result.success(result);
    }

    // ========== Request DTOs ==========

    @Data
    public static class ReviewRequest {
        private String comment;
    }
}

