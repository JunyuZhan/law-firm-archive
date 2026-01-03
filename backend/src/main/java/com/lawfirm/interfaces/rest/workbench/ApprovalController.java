package com.lawfirm.interfaces.rest.workbench;

import com.lawfirm.application.workbench.command.ApproveCommand;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.application.workbench.dto.ApprovalQueryDTO;
import com.lawfirm.application.workbench.service.ApprovalAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批中心 Controller
 */
@Tag(name = "审批中心", description = "审批管理相关接口")
@RestController
@RequestMapping("/workbench/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalAppService approvalAppService;

    /**
     * 分页查询审批记录
     */
    @Operation()
    @GetMapping("/list")
    @RequirePermission("approval:list")
    public Result<PageResult<ApprovalDTO>> listApprovals(ApprovalQueryDTO query) {
        PageResult<ApprovalDTO> result = approvalAppService.listApprovals(query);
        return Result.success(result);
    }

    /**
     * 获取待审批列表
     */
    @Operation()
    @GetMapping("/pending")
    @RequirePermission("approval:list")
    public Result<List<ApprovalDTO>> getPendingApprovals() {
        List<ApprovalDTO> approvals = approvalAppService.getPendingApprovals();
        return Result.success(approvals);
    }

    /**
     * 获取我发起的审批
     */
    @Operation()
    @GetMapping("/my-initiated")
    @RequirePermission("approval:list")
    public Result<List<ApprovalDTO>> getMyInitiatedApprovals() {
        List<ApprovalDTO> approvals = approvalAppService.getMyInitiatedApprovals();
        return Result.success(approvals);
    }

    /**
     * 获取审批详情
     */
    @Operation()
    @GetMapping("/{id}")
    @RequirePermission("approval:list")
    public Result<ApprovalDTO> getApproval(@PathVariable Long id) {
        ApprovalDTO approval = approvalAppService.getApprovalById(id);
        return Result.success(approval);
    }

    /**
     * 审批操作（通过/拒绝）
     */
    @Operation()
    @PostMapping("/approve")
    @RequirePermission("approval:approve")
    @OperationLog(module = "审批中心", action = "审批操作")
    public Result<Void> approve(@RequestBody @Valid ApproveCommand command) {
        approvalAppService.approve(command);
        return Result.success();
    }

    /**
     * 批量审批
     */
    @Operation(summary = "批量审批", description = "一次性审批多个待审批事项")
    @PostMapping("/batch-approve")
    @RequirePermission("approval:approve")
    @OperationLog(module = "审批中心", action = "批量审批")
    public Result<ApprovalAppService.BatchApproveResult> batchApprove(@RequestBody @Valid BatchApproveRequest request) {
        ApprovalAppService.BatchApproveResult result = approvalAppService.batchApprove(
                request.getApprovalIds(), 
                request.getResult(), 
                request.getComment()
        );
        return Result.success(result);
    }

    /**
     * 获取业务审批记录
     */
    @Operation()
    @GetMapping("/business")
    @RequirePermission("approval:list")
    public Result<List<ApprovalDTO>> getBusinessApprovals(
            @RequestParam String businessType,
            @RequestParam Long businessId) {
        List<ApprovalDTO> approvals = approvalAppService.getBusinessApprovals(businessType, businessId);
        return Result.success(approvals);
    }

    // ========== Request DTOs ==========

    @Data
    public static class BatchApproveRequest {
        @jakarta.validation.constraints.NotEmpty(message = "审批ID列表不能为空")
        private List<Long> approvalIds;
        
        @jakarta.validation.constraints.NotBlank(message = "审批结果不能为空")
        private String result;  // APPROVED 或 REJECTED
        
        private String comment;  // 审批意见（可选）
    }
}

