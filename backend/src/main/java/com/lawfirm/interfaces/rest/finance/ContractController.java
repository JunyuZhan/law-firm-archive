package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.command.UpdateContractCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.finance.service.ContractAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
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
     * 分页查询合同列表
     */
    @GetMapping("/list")
    @RequirePermission("contract:list")
    public Result<PageResult<ContractDTO>> listContracts(ContractQueryDTO query) {
        PageResult<ContractDTO> result = contractAppService.listContracts(query);
        return Result.success(result);
    }

    /**
     * 获取合同详情
     */
    @GetMapping("/{id}")
    @RequirePermission("contract:list")
    public Result<ContractDTO> getContract(@PathVariable Long id) {
        ContractDTO contract = contractAppService.getContractById(id);
        return Result.success(contract);
    }

    /**
     * 创建合同
     */
    @PostMapping
    @RequirePermission("contract:create")
    @OperationLog(module = "合同管理", action = "创建合同")
    public Result<ContractDTO> createContract(@RequestBody @Valid CreateContractCommand command) {
        ContractDTO contract = contractAppService.createContract(command);
        return Result.success(contract);
    }

    /**
     * 更新合同
     */
    @PutMapping
    @RequirePermission("contract:update")
    @OperationLog(module = "合同管理", action = "更新合同")
    public Result<ContractDTO> updateContract(@RequestBody @Valid UpdateContractCommand command) {
        ContractDTO contract = contractAppService.updateContract(command);
        return Result.success(contract);
    }

    /**
     * 删除合同
     */
    @DeleteMapping("/{id}")
    @RequirePermission("contract:delete")
    @OperationLog(module = "合同管理", action = "删除合同")
    public Result<Void> deleteContract(@PathVariable Long id) {
        contractAppService.deleteContract(id);
        return Result.success();
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/submit")
    @RequirePermission("contract:submit")
    @OperationLog(module = "合同管理", action = "提交审批")
    public Result<Void> submitForApproval(@PathVariable Long id) {
        contractAppService.submitForApproval(id);
        return Result.success();
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("contract:approve")
    @OperationLog(module = "合同管理", action = "审批通过")
    public Result<Void> approve(@PathVariable Long id) {
        contractAppService.approve(id);
        return Result.success();
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("contract:approve")
    @OperationLog(module = "合同管理", action = "审批拒绝")
    public Result<Void> reject(@PathVariable Long id, @RequestBody RejectRequest request) {
        contractAppService.reject(id, request.getReason());
        return Result.success();
    }

    /**
     * 终止合同
     */
    @PostMapping("/{id}/terminate")
    @RequirePermission("contract:terminate")
    @OperationLog(module = "合同管理", action = "终止合同")
    public Result<Void> terminate(@PathVariable Long id, @RequestBody RejectRequest request) {
        contractAppService.terminate(id, request.getReason());
        return Result.success();
    }

    /**
     * 完成合同
     */
    @PostMapping("/{id}/complete")
    @RequirePermission("contract:complete")
    @OperationLog(module = "合同管理", action = "完成合同")
    public Result<Void> complete(@PathVariable Long id) {
        contractAppService.complete(id);
        return Result.success();
    }

    // ========== Request DTOs ==========

    @Data
    public static class RejectRequest {
        private String reason;
    }
}

