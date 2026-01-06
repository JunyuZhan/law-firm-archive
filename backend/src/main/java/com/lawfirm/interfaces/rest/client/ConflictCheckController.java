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
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
     * 创建利冲检查（关联案件）
     */
    @PostMapping
    @RequirePermission("conflict:create")
    @OperationLog(module = "利冲检查", action = "创建利冲检查")
    public Result<ConflictCheckDTO> createConflictCheck(@RequestBody @Valid CreateConflictCheckCommand command) {
        ConflictCheckDTO check = conflictCheckAppService.createConflictCheck(command);
        return Result.success(check);
    }

    /**
     * 申请利冲审查（简化版，手动申请）
     * 用于前端手动申请利冲检查，不需要关联案件
     */
    @PostMapping("/apply")
    @RequirePermission("conflict:create")
    @OperationLog(module = "利冲检查", action = "申请利冲审查")
    public Result<ConflictCheckDTO> applyConflictCheck(@RequestBody @Valid ApplyConflictCheckRequest request) {
        ConflictCheckDTO check = conflictCheckAppService.applyConflictCheck(
                request.getClientName(),
                request.getOpposingParty(),
                request.getMatterName(),
                request.getCheckType(),
                request.getRemark()
        );
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

    /**
     * 批准豁免申请
     */
    @PostMapping("/exemption/{id}/approve")
    @RequirePermission("conflict:approve")
    @OperationLog(module = "利冲检查", action = "批准豁免")
    public Result<Void> approveExemption(@PathVariable Long id, @RequestBody ReviewRequest request) {
        conflictCheckAppService.approveExemption(id, request.getComment());
        return Result.success();
    }

    /**
     * 拒绝豁免申请
     */
    @PostMapping("/exemption/{id}/reject")
    @RequirePermission("conflict:approve")
    @OperationLog(module = "利冲检查", action = "拒绝豁免")
    public Result<Void> rejectExemption(@PathVariable Long id, @RequestBody ReviewRequest request) {
        conflictCheckAppService.rejectExemption(id, request.getComment());
        return Result.success();
    }

    /**
     * 快速利冲检索（不创建记录，仅检查是否存在冲突）
     * 用于新增客户时的实时检查
     */
    @PostMapping("/quick")
    @RequirePermission("conflict:list")
    public Result<QuickConflictCheckResponse> quickConflictCheck(@RequestBody @Valid QuickConflictCheckRequest request) {
        var result = conflictCheckAppService.quickConflictCheck(
                request.getClientName(),
                request.getOpposingParty()
        );
        QuickConflictCheckResponse response = new QuickConflictCheckResponse();
        response.setHasConflict(result.hasConflict());
        response.setConflictDetail(result.conflictDetail());
        return Result.success(response);
    }

    // ========== Request DTOs ==========

    @Data
    public static class ReviewRequest {
        private String comment;
    }

    @Data
    public static class ApplyConflictCheckRequest {
        @NotBlank(message = "客户名称不能为空")
        private String clientName;
        
        @NotBlank(message = "对方当事人不能为空")
        private String opposingParty;
        
        private String matterName;
        
        private String checkType;
        
        private String remark;
    }

    @Data
    public static class QuickConflictCheckRequest {
        @NotBlank(message = "客户名称不能为空")
        private String clientName;
        
        @NotBlank(message = "对方当事人不能为空")
        private String opposingParty;
    }

    @Data
    public static class QuickConflictCheckResponse {
        private boolean hasConflict;
        private String conflictDetail;
    }
}

