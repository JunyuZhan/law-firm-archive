package com.lawfirm.interfaces.rest.ai;

import com.lawfirm.application.ai.command.AiBillOperationCommand;
import com.lawfirm.application.ai.command.GenerateMonthlyBillCommand;
import com.lawfirm.application.ai.command.SalaryDeductionLinkCommand;
import com.lawfirm.application.ai.dto.AiMonthlyBillDTO;
import com.lawfirm.application.ai.dto.AiUsageLogDTO;
import com.lawfirm.application.ai.dto.AiUsageQueryDTO;
import com.lawfirm.application.ai.dto.AiUsageSummaryDTO;
import com.lawfirm.application.ai.service.AiBillingAppService;
import com.lawfirm.application.ai.service.AiUsageAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * AI使用量 Controller
 */
@Slf4j
@Tag(name = "AI使用量管理", description = "AI使用量记录与费用查询接口")
@RestController
@RequestMapping("/ai/usage")
@RequiredArgsConstructor
public class AiUsageController {

    private final AiUsageAppService usageAppService;
    private final AiBillingAppService billingAppService;

    // ==================== 个人使用量查询 ====================

    /**
     * 我的使用记录
     */
    @Operation(summary = "我的使用记录", description = "查询当前用户的AI使用记录")
    @GetMapping("/my")
    @RequirePermission("ai:usage:view")
    public Result<PageResult<AiUsageLogDTO>> getMyUsageLogs(AiUsageQueryDTO query) {
        return Result.success(usageAppService.getMyUsageLogs(query));
    }

    /**
     * 我的使用统计
     */
    @Operation(summary = "我的使用统计", description = "获取当前用户的月度使用统计")
    @GetMapping("/my/summary")
    @RequirePermission("ai:usage:view")
    public Result<AiUsageSummaryDTO> getMyUsageSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return Result.success(usageAppService.getMyUsageSummary(month));
    }

    /**
     * 我的模型使用分布
     */
    @Operation(summary = "我的模型使用分布", description = "获取当前用户按模型分组的使用统计")
    @GetMapping("/my/by-model")
    @RequirePermission("ai:usage:view")
    public Result<List<Map<String, Object>>> getMyUsageByModel(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        if (month == null) {
            month = YearMonth.now();
        }
        return Result.success(usageAppService.getUsageByModel(userId, month));
    }

    /**
     * 我的使用趋势
     */
    @Operation(summary = "我的使用趋势", description = "获取当前用户按日的使用趋势")
    @GetMapping("/my/trend")
    @RequirePermission("ai:usage:view")
    public Result<List<Map<String, Object>>> getMyUsageTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        if (month == null) {
            month = YearMonth.now();
        }
        return Result.success(usageAppService.getUsageTrend(userId, month));
    }

    /**
     * 我的配额信息
     */
    @Operation(summary = "我的配额信息", description = "获取当前用户的配额使用情况")
    @GetMapping("/my/quota")
    @RequirePermission("ai:usage:view")
    public Result<com.lawfirm.domain.ai.entity.AiUserQuota> getMyQuota() {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        return Result.success(usageAppService.getUserQuota(userId));
    }

    // ==================== 管理员统计 ====================

    /**
     * 全员使用统计
     */
    @Operation(summary = "全员使用统计", description = "管理员查看所有用户的AI使用统计")
    @GetMapping("/statistics")
    @RequirePermission("ai:billing:view")
    public Result<List<AiUsageSummaryDTO>> getAllUsersSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        return Result.success(usageAppService.getAllUsersSummary(month));
    }

    /**
     * 部门使用统计
     */
    @Operation(summary = "部门使用统计", description = "按部门统计AI使用情况")
    @GetMapping("/statistics/department")
    @RequirePermission("ai:billing:view")
    public Result<List<Map<String, Object>>> getDepartmentSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        if (month == null) {
            month = YearMonth.now();
        }
        return Result.success(usageAppService.getDepartmentSummary(month));
    }

    // ==================== 账单管理 ====================

    /**
     * 月度账单列表
     */
    @Operation(summary = "月度账单列表", description = "查询指定月份的用户账单")
    @GetMapping("/billing")
    @RequirePermission("ai:billing:view")
    public Result<List<AiMonthlyBillDTO>> getMonthlyBills(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(billingAppService.getMonthlyBills(year, month));
    }

    /**
     * 获取我的账单
     */
    @Operation(summary = "获取我的账单", description = "获取当前用户指定月份的账单")
    @GetMapping("/billing/my")
    @RequirePermission("ai:usage:view")
    public Result<AiMonthlyBillDTO> getMyBill(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = com.lawfirm.common.util.SecurityUtils.getUserId();
        return Result.success(billingAppService.getUserBill(userId, year, month));
    }

    /**
     * 生成月度账单
     */
    @Operation(summary = "生成月度账单", description = "为指定月份生成所有用户的AI费用账单")
    @PostMapping("/billing/generate")
    @RequirePermission("ai:billing:manage")
    @OperationLog(module = "AI账单管理", action = "生成月度账单")
    public Result<Integer> generateMonthlyBills(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int count = billingAppService.generateMonthlyBills(year, month);
        return Result.success(count);
    }

    /**
     * 标记已扣减
     */
    @Operation(summary = "标记已扣减", description = "标记账单已完成工资扣减")
    @PostMapping("/billing/{id}/deduct")
    @RequirePermission("ai:billing:manage")
    @OperationLog(module = "AI账单管理", action = "标记账单已扣减")
    public Result<Void> markDeducted(@PathVariable Long id,
                                      @RequestParam(required = false) String remark) {
        billingAppService.markDeducted(id, remark);
        return Result.success();
    }

    /**
     * 减免费用
     */
    @Operation(summary = "减免费用", description = "减免用户的AI费用（不扣减）")
    @PostMapping("/billing/{id}/waive")
    @RequirePermission("ai:billing:manage")
    @OperationLog(module = "AI账单管理", action = "减免账单")
    public Result<Void> waiveBill(@PathVariable Long id,
                                   @RequestParam(required = false) String reason) {
        billingAppService.waiveBill(id, reason);
        return Result.success();
    }

    /**
     * 关联工资扣减
     */
    @Operation(summary = "关联工资扣减", description = "将AI费用账单关联到工资扣减记录")
    @PostMapping("/billing/link-salary")
    @RequirePermission("ai:billing:manage")
    @OperationLog(module = "AI账单管理", action = "关联工资扣减")
    public Result<Void> linkToSalaryDeduction(@RequestBody @Valid SalaryDeductionLinkCommand command) {
        billingAppService.linkToSalaryDeduction(command);
        return Result.success();
    }
}
