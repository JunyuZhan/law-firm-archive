package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreateCommissionRuleCommand;
import com.lawfirm.application.finance.command.ManualCalculateCommissionCommand;
import com.lawfirm.application.finance.command.UpdateCommissionRuleCommand;
import com.lawfirm.application.finance.dto.CommissionDTO;
import com.lawfirm.application.finance.dto.CommissionQueryDTO;
import com.lawfirm.application.finance.dto.CommissionRuleDTO;
import com.lawfirm.application.finance.dto.PaymentDTO;
import com.lawfirm.application.finance.service.CommissionAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 提成管理控制器
 */
@RestController
@RequestMapping("/finance/commission")
@RequiredArgsConstructor
@Tag(name = "提成管理", description = "提成规则配置和提成计算")
public class CommissionController {

    private final CommissionAppService commissionAppService;

    // ========== 提成规则管理 ==========

    @PostMapping("/rules")
    @RequirePermission("finance:commission:rule:create")
    @Operation(summary = "创建提成规则", description = "仅系统管理员和主任可以创建")
    @OperationLog(module = "提成管理", action = "创建提成规则")
    public Result<CommissionRuleDTO> createRule(@RequestBody @Valid CreateCommissionRuleCommand command) {
        CommissionRuleDTO rule = commissionAppService.createCommissionRule(command);
        return Result.success(rule);
    }

    @PutMapping("/rules/{id}")
    @RequirePermission("finance:commission:rule:update")
    @Operation(summary = "更新提成规则", description = "仅系统管理员和主任可以更新")
    @OperationLog(module = "提成管理", action = "更新提成规则")
    public Result<CommissionRuleDTO> updateRule(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCommissionRuleCommand command) {
        CommissionRuleDTO rule = commissionAppService.updateCommissionRule(id, command);
        return Result.success(rule);
    }

    @DeleteMapping("/rules/{id}")
    @RequirePermission("finance:commission:rule:delete")
    @Operation(summary = "删除提成规则", description = "不能删除默认规则")
    @OperationLog(module = "提成管理", action = "删除提成规则")
    public Result<Void> deleteRule(@PathVariable Long id) {
        commissionAppService.deleteCommissionRule(id);
        return Result.success();
    }

    @GetMapping("/rules/{id}")
    @RequirePermission("finance:commission:rule:view")
    @Operation(summary = "获取提成规则详情")
    public Result<CommissionRuleDTO> getRule(@PathVariable Long id) {
        CommissionRuleDTO rule = commissionAppService.getCommissionRule(id);
        return Result.success(rule);
    }

    @GetMapping("/rules")
    @RequirePermission("finance:commission:rule:list")
    @Operation(summary = "查询提成规则列表")
    public Result<List<CommissionRuleDTO>> listRules() {
        List<CommissionRuleDTO> rules = commissionAppService.listCommissionRules();
        return Result.success(rules);
    }

    @PutMapping("/rules/{id}/set-default")
    @RequirePermission("finance:commission:rule:update")
    @Operation(summary = "设为默认规则", description = "仅系统管理员和主任可以操作")
    @OperationLog(module = "提成管理", action = "设为默认规则")
    public Result<Void> setDefaultRule(@PathVariable Long id) {
        commissionAppService.setDefaultRule(id);
        return Result.success();
    }

    @PutMapping("/rules/{id}/toggle")
    @RequirePermission("finance:commission:rule:update")
    @Operation(summary = "切换规则状态", description = "启用/停用规则")
    @OperationLog(module = "提成管理", action = "切换规则状态")
    public Result<Void> toggleRule(@PathVariable Long id) {
        commissionAppService.toggleRule(id);
        return Result.success();
    }

    // ========== 提成计算 ==========

    @PostMapping("/calculate/{paymentId}")
    @RequirePermission("finance:commission:calculate")
    @Operation(summary = "计算提成", description = "收款核销后自动触发，也可手动触发")
    @OperationLog(module = "提成管理", action = "计算提成")
    public Result<List<CommissionDTO>> calculateCommission(@PathVariable Long paymentId) {
        List<CommissionDTO> commissions = commissionAppService.calculateCommission(paymentId);
        return Result.success(commissions);
    }

    @PostMapping("/manual-calculate")
    @RequirePermission("finance:commission:calculate")
    @Operation(summary = "手动计算提成", description = "财务用户手动计算提成，可修改提成比例和金额")
    @OperationLog(module = "提成管理", action = "手动计算提成")
    public Result<List<CommissionDTO>> manualCalculateCommission(@RequestBody @Valid ManualCalculateCommissionCommand command) {
        List<CommissionDTO> commissions = commissionAppService.manualCalculateCommission(command);
        return Result.success(commissions);
    }

    // ========== 提成查询 ==========

    @GetMapping
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "查询提成记录列表")
    public Result<PageResult<CommissionDTO>> listCommissions(CommissionQueryDTO query) {
        PageResult<CommissionDTO> result = commissionAppService.listCommissions(query);
        return Result.success(result);
    }

    @GetMapping("/detail/{id}")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "获取提成记录详情")
    public Result<CommissionDTO> getCommission(@PathVariable Long id) {
        CommissionDTO commission = commissionAppService.getCommission(id);
        return Result.success(commission);
    }

    @GetMapping("/users/{userId}/total")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "查询用户提成总额")
    public Result<BigDecimal> getUserTotalCommission(@PathVariable Long userId) {
        BigDecimal total = commissionAppService.getUserTotalCommission(userId);
        return Result.success(total);
    }

    @GetMapping("/pending-payments")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "获取待计算提成的收款记录列表", description = "返回所有已确认但还没有生成提成记录的收款记录")
    public Result<List<PaymentDTO>> getPendingCommissionPayments() {
        List<PaymentDTO> payments = commissionAppService.getPendingCommissionPayments();
        return Result.success(payments);
    }

    // ========== 提成汇总查看（M4-037，P1） ==========

    @GetMapping("/summary")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "获取全所提成汇总", description = "仅管理层可以查看")
    public Result<Map<String, Object>> getCommissionSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> summary = commissionAppService.getCommissionSummary(startDate, endDate);
        return Result.success(summary);
    }

    // ========== 提成审批（M4-038，P1） ==========

    @PostMapping("/{id}/approve")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "审批提成", description = "仅主任可以审批")
    @OperationLog(module = "提成管理", action = "审批提成")
    public Result<CommissionDTO> approveCommission(
            @PathVariable Long id,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String comment) {
        CommissionDTO commission = commissionAppService.approveCommission(id, approved, comment);
        return Result.success(commission);
    }

    @PostMapping("/batch-approve")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "批量审批提成", description = "仅主任可以审批")
    @OperationLog(module = "提成管理", action = "批量审批提成")
    public Result<Void> batchApproveCommissions(
            @RequestBody List<Long> ids,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String comment) {
        commissionAppService.batchApproveCommissions(ids, approved, comment);
        return Result.success();
    }

    // ========== 提成发放（M4-039，P1） ==========

    @PostMapping("/{id}/issue")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "确认提成已发放", description = "仅财务人员可以确认")
    @OperationLog(module = "提成管理", action = "确认提成发放")
    public Result<CommissionDTO> issueCommission(@PathVariable Long id) {
        CommissionDTO commission = commissionAppService.issueCommission(id);
        return Result.success(commission);
    }

    @PostMapping("/batch-issue")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "批量确认提成发放", description = "仅财务人员可以确认")
    @OperationLog(module = "提成管理", action = "批量确认提成发放")
    public Result<Void> batchIssueCommissions(@RequestBody List<Long> ids) {
        commissionAppService.batchIssueCommissions(ids);
        return Result.success();
    }

    // ========== 提成报表（M4-040，P1） ==========

    @GetMapping("/report")
    @RequirePermission("finance:commission:manage")
    @Operation(summary = "生成提成报表", description = "财务和管理层可以查看")
    public Result<List<Map<String, Object>>> getCommissionReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long userId) {
        List<Map<String, Object>> report = commissionAppService.getCommissionReportData(startDate, endDate, userId);
        return Result.success(report);
    }
}

