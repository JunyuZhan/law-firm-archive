package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.AllocateCostCommand;
import com.lawfirm.application.finance.command.ApproveExpenseCommand;
import com.lawfirm.application.finance.command.CreateExpenseCommand;
import com.lawfirm.application.finance.command.SplitCostCommand;
import com.lawfirm.application.finance.dto.CostSplitDTO;
import com.lawfirm.application.finance.dto.CostAllocationDTO;
import com.lawfirm.application.finance.dto.ExpenseDTO;
import com.lawfirm.application.finance.dto.ExpenseQueryDTO;
import com.lawfirm.application.finance.service.ExpenseAppService;
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

/**
 * 费用报销控制器
 */
@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "费用报销管理", description = "费用报销申请、审批、成本归集")
public class ExpenseController {

    private final ExpenseAppService expenseAppService;

    @GetMapping
    @RequirePermission("finance:expense:list")
    @Operation(summary = "查询费用报销列表")
    public Result<PageResult<ExpenseDTO>> listExpenses(ExpenseQueryDTO query) {
        PageResult<ExpenseDTO> result = expenseAppService.listExpenses(query);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @RequirePermission("finance:expense:view")
    @Operation(summary = "获取费用报销详情")
    public Result<ExpenseDTO> getExpense(@PathVariable Long id) {
        ExpenseDTO expense = expenseAppService.getExpense(id);
        return Result.success(expense);
    }

    @PostMapping
    @RequirePermission("finance:expense:apply")
    @Operation(summary = "创建费用报销申请")
    @OperationLog(module = "费用报销", action = "创建报销申请")
    public Result<ExpenseDTO> createExpense(@RequestBody @Valid CreateExpenseCommand command) {
        ExpenseDTO expense = expenseAppService.createExpense(command);
        return Result.success(expense);
    }

    @PostMapping("/{id}/approve")
    @RequirePermission("finance:expense:approve")
    @Operation(summary = "审批费用报销")
    @OperationLog(module = "费用报销", action = "审批报销")
    public Result<ExpenseDTO> approveExpense(@PathVariable Long id, 
                                            @RequestBody @Valid ApproveExpenseCommand command) {
        command.setExpenseId(id);
        ExpenseDTO expense = expenseAppService.approveExpense(command);
        return Result.success(expense);
    }

    @PostMapping("/{id}/pay")
    @RequirePermission("finance:expense:pay")
    @Operation(summary = "确认支付")
    @OperationLog(module = "费用报销", action = "确认支付")
    public Result<ExpenseDTO> confirmPayment(@PathVariable Long id, 
                                            @RequestParam String paymentMethod) {
        ExpenseDTO expense = expenseAppService.confirmPayment(id, paymentMethod);
        return Result.success(expense);
    }

    @PostMapping("/allocate-cost")
    @RequirePermission("finance:expense:allocate")
    @Operation(summary = "成本归集", description = "将已支付的费用归集到项目成本")
    @OperationLog(module = "费用报销", action = "成本归集")
    public Result<Void> allocateCost(@RequestBody @Valid AllocateCostCommand command) {
        expenseAppService.allocateCost(command);
        return Result.success();
    }

    @GetMapping("/matters/{matterId}/costs")
    @RequirePermission("finance:expense:view")
    @Operation(summary = "查询项目成本归集记录")
    public Result<List<CostAllocationDTO>> listCostAllocations(@PathVariable Long matterId) {
        List<CostAllocationDTO> allocations = expenseAppService.listCostAllocations(matterId);
        return Result.success(allocations);
    }

    @GetMapping("/matters/{matterId}/total-cost")
    @RequirePermission("finance:expense:view")
    @Operation(summary = "获取项目总成本")
    public Result<BigDecimal> getTotalCost(@PathVariable Long matterId) {
        BigDecimal totalCost = expenseAppService.getTotalCost(matterId);
        return Result.success(totalCost);
    }

    @DeleteMapping("/{id}")
    @RequirePermission("finance:expense:delete")
    @Operation(summary = "删除费用报销")
    @OperationLog(module = "费用报销", action = "删除报销")
    public Result<Void> deleteExpense(@PathVariable Long id) {
        expenseAppService.deleteExpense(id);
        return Result.success();
    }

    @PostMapping("/split-cost")
    @RequirePermission("finance:expense:split")
    @Operation(summary = "成本分摊", description = "将公共成本分摊到多个项目（M4-043）")
    @OperationLog(module = "费用报销", action = "成本分摊")
    public Result<Void> splitCost(@RequestBody @Valid SplitCostCommand command) {
        expenseAppService.splitCost(command);
        return Result.success();
    }

    @GetMapping("/matters/{matterId}/splits")
    @RequirePermission("finance:expense:view")
    @Operation(summary = "查询项目成本分摊记录")
    public Result<List<CostSplitDTO>> listCostSplits(@PathVariable Long matterId) {
        List<CostSplitDTO> splits = expenseAppService.listCostSplits(matterId);
        return Result.success(splits);
    }
}

