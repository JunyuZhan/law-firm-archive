package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.AllocateCostCommand;
import com.lawfirm.application.finance.command.ApproveExpenseCommand;
import com.lawfirm.application.finance.command.CreateExpenseCommand;
import com.lawfirm.application.finance.command.SplitCostCommand;
import com.lawfirm.application.finance.dto.CostAllocationDTO;
import com.lawfirm.application.finance.dto.CostSplitDTO;
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
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 费用报销控制器 */
@RestController
@RequestMapping("/finance/expense")
@RequiredArgsConstructor
@Tag(name = "费用报销管理", description = "费用报销申请、审批、成本归集")
public class ExpenseController {

  /** 费用报销应用服务. */
  private final ExpenseAppService expenseAppService;

  /**
   * 查询费用报销列表
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission(
      value = {"finance:expense:list", "finance:report:view"},
      logical = RequirePermission.Logical.OR)
  @Operation(summary = "查询费用报销列表")
  public Result<PageResult<ExpenseDTO>> listExpenses(final ExpenseQueryDTO query) {
    PageResult<ExpenseDTO> result = expenseAppService.listExpenses(query);
    return Result.success(result);
  }

  /**
   * 获取费用报销详情
   *
   * @param id 费用报销ID
   * @return 费用报销详情
   */
  @GetMapping("/{id}")
  @RequirePermission("finance:expense:view")
  @Operation(summary = "获取费用报销详情")
  public Result<ExpenseDTO> getExpense(@PathVariable final Long id) {
    ExpenseDTO expense = expenseAppService.getExpense(id);
    return Result.success(expense);
  }

  /**
   * 创建费用报销申请
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping
  @RequirePermission("finance:expense:apply")
  @Operation(summary = "创建费用报销申请")
  @OperationLog(module = "费用报销", action = "创建报销申请")
  public Result<ExpenseDTO> createExpense(@RequestBody @Valid final CreateExpenseCommand command) {
    ExpenseDTO expense = expenseAppService.createExpense(command);
    return Result.success(expense);
  }

  /**
   * 审批费用报销
   *
   * @param id 费用报销ID
   * @param command 审批命令
   * @return 审批结果
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("finance:expense:approve")
  @Operation(summary = "审批费用报销")
  @OperationLog(module = "费用报销", action = "审批报销")
  public Result<ExpenseDTO> approveExpense(
      @PathVariable final Long id, @RequestBody @Valid final ApproveExpenseCommand command) {
    command.setExpenseId(id);
    ExpenseDTO expense = expenseAppService.approveExpense(command);
    return Result.success(expense);
  }

  /**
   * 确认支付
   *
   * @param id 费用报销ID
   * @param paymentMethod 支付方式
   * @return 确认支付结果
   */
  @PostMapping("/{id}/pay")
  @RequirePermission("finance:expense:pay")
  @Operation(summary = "确认支付")
  @OperationLog(module = "费用报销", action = "确认支付")
  public Result<ExpenseDTO> confirmPayment(
      @PathVariable final Long id, @RequestParam final String paymentMethod) {
    ExpenseDTO expense = expenseAppService.confirmPayment(id, paymentMethod);
    return Result.success(expense);
  }

  /**
   * 成本归集
   *
   * @param command 成本归集命令
   * @return 操作结果
   */
  @PostMapping("/allocate-cost")
  @RequirePermission("finance:expense:allocate")
  @Operation(summary = "成本归集", description = "将已支付的费用归集到项目成本")
  @OperationLog(module = "费用报销", action = "成本归集")
  public Result<Void> allocateCost(@RequestBody @Valid final AllocateCostCommand command) {
    expenseAppService.allocateCost(command);
    return Result.success();
  }

  /**
   * 查询项目成本归集记录
   *
   * @param matterId 项目ID
   * @return 成本归集记录列表
   */
  @GetMapping("/matters/{matterId}/costs")
  @RequirePermission("finance:expense:view")
  @Operation(summary = "查询项目成本归集记录")
  public Result<List<CostAllocationDTO>> listCostAllocations(@PathVariable final Long matterId) {
    List<CostAllocationDTO> allocations = expenseAppService.listCostAllocations(matterId);
    return Result.success(allocations);
  }

  /** 获取项目总成本 */
  /**
   * 获取项目总成本
   *
   * @param matterId 项目ID
   * @return 总成本
   */
  @GetMapping("/matters/{matterId}/total-cost")
  @RequirePermission("finance:expense:view")
  @Operation(summary = "获取项目总成本")
  public Result<BigDecimal> getTotalCost(@PathVariable final Long matterId) {
    BigDecimal totalCost = expenseAppService.getTotalCost(matterId);
    return Result.success(totalCost);
  }

  /**
   * 删除费用报销
   *
   * @param id 报销ID
   * @return 操作结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("finance:expense:delete")
  @Operation(summary = "删除费用报销")
  @OperationLog(module = "费用报销", action = "删除报销")
  public Result<Void> deleteExpense(@PathVariable final Long id) {
    expenseAppService.deleteExpense(id);
    return Result.success();
  }

  /**
   * 成本分摊
   *
   * @param command 分摊命令
   * @return 操作结果
   */
  @PostMapping("/split-cost")
  @RequirePermission("finance:expense:split")
  @Operation(summary = "成本分摊", description = "将公共成本分摊到多个项目（M4-043）")
  @OperationLog(module = "费用报销", action = "成本分摊")
  public Result<Void> splitCost(@RequestBody @Valid final SplitCostCommand command) {
    expenseAppService.splitCost(command);
    return Result.success();
  }

  /**
   * 查询项目成本分摊记录
   *
   * @param matterId 项目ID
   * @return 分摊记录列表
   */
  @GetMapping("/matters/{matterId}/splits")
  @RequirePermission("finance:expense:view")
  @Operation(summary = "查询项目成本分摊记录")
  public Result<List<CostSplitDTO>> listCostSplits(@PathVariable final Long matterId) {
    List<CostSplitDTO> splits = expenseAppService.listCostSplits(matterId);
    return Result.success(splits);
  }
}
