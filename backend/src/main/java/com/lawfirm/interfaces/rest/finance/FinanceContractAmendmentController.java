package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.service.FinanceContractAmendmentService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.finance.entity.FinanceContractAmendment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财务合同变更管理控制器
 * 
 * 用于财务人员处理律师变更合同后的数据同步
 */
@Tag(name = "财务合同变更管理")
@RestController
@RequestMapping("/finance/contract-amendments")
@RequiredArgsConstructor
public class FinanceContractAmendmentController {

    private final FinanceContractAmendmentService amendmentService;

    @Operation(summary = "查询待处理的变更记录")
    @GetMapping("/pending")
    @RequirePermission("finance:contract:amendment:view")
    public Result<List<FinanceContractAmendment>> getPendingAmendments() {
        List<FinanceContractAmendment> amendments = amendmentService.getPendingAmendments();
        return Result.success(amendments);
    }

    @Operation(summary = "查询合同的变更记录")
    @GetMapping("/contract/{contractId}")
    @RequirePermission("finance:contract:amendment:view")
    public Result<List<FinanceContractAmendment>> getAmendmentsByContractId(
            @PathVariable Long contractId) {
        List<FinanceContractAmendment> amendments = amendmentService.getAmendmentsByContractId(contractId);
        return Result.success(amendments);
    }

    @Operation(summary = "同步变更到财务数据")
    @PostMapping("/{amendmentId}/sync")
    @RequirePermission("finance:contract:amendment:sync")
    public Result<Void> syncAmendment(
            @PathVariable Long amendmentId,
            @RequestParam(required = false) String remark) {
        amendmentService.syncAmendment(amendmentId, remark);
        return Result.success();
    }

    @Operation(summary = "忽略变更（不同步）")
    @PostMapping("/{amendmentId}/ignore")
    @RequirePermission("finance:contract:amendment:sync")
    public Result<Void> ignoreAmendment(
            @PathVariable Long amendmentId,
            @RequestParam String remark) {
        amendmentService.ignoreAmendment(amendmentId, remark);
        return Result.success();
    }
}
