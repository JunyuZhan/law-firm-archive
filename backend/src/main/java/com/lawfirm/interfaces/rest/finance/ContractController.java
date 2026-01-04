package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 合同管理 Controller
 */
@RestController("financeContractController")
@RequestMapping("/finance/contract")
@RequiredArgsConstructor
public class ContractController {

    private final ContractAppService contractAppService;

    /**
     * 分页查询合同列表（财务模块：只读）
     */
    @GetMapping("/list")
    @RequirePermission("contract:view")
    public Result<PageResult<ContractDTO>> listContracts(ContractQueryDTO query) {
        PageResult<ContractDTO> result = contractAppService.listContracts(query);
        return Result.success(result);
    }

    /**
     * 获取合同详情（财务模块：只读）
     */
    @GetMapping("/{id}")
    @RequirePermission("contract:view")
    public Result<ContractDTO> getContract(@PathVariable Long id) {
        ContractDTO contract = contractAppService.getContractById(id);
        return Result.success(contract);
    }

    /**
     * 提交审批（保留此接口，供项目管理模块调用）
     */
    @PostMapping("/{id}/submit")
    @RequirePermission("matter:contract:submit")
    @OperationLog(module = "合同管理", action = "提交审批")
    public Result<Void> submitForApproval(
            @PathVariable Long id,
            @RequestParam(required = false) Long approverId) {
        contractAppService.submitForApproval(id, approverId);
        return Result.success();
    }

    /**
     * 审批通过（保留此接口，供审批中心调用）
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("contract:approve")
    @OperationLog(module = "合同管理", action = "审批通过")
    public Result<Void> approve(@PathVariable Long id) {
        contractAppService.approve(id);
        return Result.success();
    }

    /**
     * 审批拒绝（保留此接口，供审批中心调用）
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("contract:approve")
    @OperationLog(module = "合同管理", action = "审批拒绝")
    public Result<Void> reject(@PathVariable Long id, @RequestBody RejectRequest request) {
        contractAppService.reject(id, request.getReason());
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class RejectRequest {
        private String reason;
    }
}

