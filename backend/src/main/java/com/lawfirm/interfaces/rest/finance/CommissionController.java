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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 提成管理控制器 */
@RestController
@RequestMapping("/finance/commission")
@RequiredArgsConstructor
@Tag(name = "提成管理", description = "提成规则配置和提成计算")
public class CommissionController {

  /** 提成应用服务. */
  private final CommissionAppService commissionAppService;

  // ========== 提成规则管理 ==========

  /**
   * 创建提成规则
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping("/rules")
  @RequirePermission("finance:commission:rule:create")
  @Operation(summary = "创建提成规则", description = "仅系统管理员和主任可以创建")
  @OperationLog(module = "提成管理", action = "创建提成规则")
  public Result<CommissionRuleDTO> createRule(
      @RequestBody @Valid final CreateCommissionRuleCommand command) {
    CommissionRuleDTO rule = commissionAppService.createCommissionRule(command);
    return Result.success(rule);
  }

  /**
   * 更新提成规则
   *
   * @param id 规则ID
   * @param command 更新命令
   * @return 更新结果
   */
  @PutMapping("/rules/{id}")
  @RequirePermission("finance:commission:rule:update")
  @Operation(summary = "更新提成规则", description = "仅系统管理员和主任可以更新")
  @OperationLog(module = "提成管理", action = "更新提成规则")
  public Result<CommissionRuleDTO> updateRule(
      @PathVariable final Long id, @RequestBody @Valid final UpdateCommissionRuleCommand command) {
    CommissionRuleDTO rule = commissionAppService.updateCommissionRule(id, command);
    return Result.success(rule);
  }

  /**
   * 删除提成规则
   *
   * @param id 规则ID
   * @return 操作结果
   */
  @DeleteMapping("/rules/{id}")
  @RequirePermission("finance:commission:rule:delete")
  @Operation(summary = "删除提成规则", description = "不能删除默认规则")
  @OperationLog(module = "提成管理", action = "删除提成规则")
  public Result<Void> deleteRule(@PathVariable final Long id) {
    commissionAppService.deleteCommissionRule(id);
    return Result.success();
  }

  /**
   * 获取提成规则详情
   *
   * @param id 规则ID
   * @return 规则详情
   */
  @GetMapping("/rules/{id}")
  @RequirePermission("finance:commission:rule:view")
  @Operation(summary = "获取提成规则详情")
  public Result<CommissionRuleDTO> getRule(@PathVariable final Long id) {
    CommissionRuleDTO rule = commissionAppService.getCommissionRule(id);
    return Result.success(rule);
  }

  /**
   * 查询提成规则列表
   *
   * @return 规则列表
   */
  @GetMapping("/rules")
  @RequirePermission("finance:commission:rule:list")
  @Operation(summary = "查询提成规则列表")
  public Result<List<CommissionRuleDTO>> listRules() {
    List<CommissionRuleDTO> rules = commissionAppService.listCommissionRules();
    return Result.success(rules);
  }

  /**
   * 获取激活的提成规则列表（公共接口） 供律师创建合同时选择提成方案使用，无需特殊权限
   *
   * @return 激活的规则列表
   */
  @GetMapping("/rules/active")
  @Operation(summary = "获取激活的提成规则列表（公共）", description = "供律师创建合同时选择提成方案使用，所有登录用户都可以访问")
  public Result<List<CommissionRuleDTO>> listActiveRules() {
    List<CommissionRuleDTO> rules = commissionAppService.listCommissionRules();
    // 只返回激活的规则
    List<CommissionRuleDTO> activeRules =
        rules.stream()
            .filter(CommissionRuleDTO::getActive)
            .collect(java.util.stream.Collectors.toList());
    return Result.success(activeRules);
  }

  /**
   * 设为默认规则
   *
   * @param id 规则ID
   * @return 操作结果
   */
  @PutMapping("/rules/{id}/set-default")
  @RequirePermission("finance:commission:rule:update")
  @Operation(summary = "设为默认规则", description = "仅系统管理员和主任可以操作")
  @OperationLog(module = "提成管理", action = "设为默认规则")
  public Result<Void> setDefaultRule(@PathVariable final Long id) {
    commissionAppService.setDefaultRule(id);
    return Result.success();
  }

  /**
   * 切换规则状态
   *
   * @param id 规则ID
   * @return 操作结果
   */
  @PutMapping("/rules/{id}/toggle")
  @RequirePermission("finance:commission:rule:update")
  @Operation(summary = "切换规则状态", description = "启用/停用规则")
  @OperationLog(module = "提成管理", action = "切换规则状态")
  public Result<Void> toggleRule(@PathVariable final Long id) {
    commissionAppService.toggleRule(id);
    return Result.success();
  }

  // ========== 提成计算 ==========

  /**
   * 手动计算提成
   *
   * @param command 计算命令
   * @return 计算结果
   */
  @PostMapping("/manual-calculate")
  @RequirePermission("finance:commission:calculate")
  @Operation(summary = "手动计算提成", description = "财务用户手动计算提成，可修改提成比例和金额")
  @OperationLog(module = "提成管理", action = "手动计算提成")
  public Result<List<CommissionDTO>> manualCalculateCommission(
      @RequestBody @Valid final ManualCalculateCommissionCommand command) {
    List<CommissionDTO> commissions = commissionAppService.manualCalculateCommission(command);
    return Result.success(commissions);
  }

  // ========== 提成查询 ==========

  /**
   * 查询提成记录列表
   *
   * @param query 查询条件
   * @return 提成列表
   */
  @GetMapping
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "查询提成记录列表")
  public Result<PageResult<CommissionDTO>> listCommissions(final CommissionQueryDTO query) {
    PageResult<CommissionDTO> result = commissionAppService.listCommissions(query);
    return Result.success(result);
  }

  /**
   * 获取提成记录详情
   *
   * @param id 提成ID
   * @return 提成详情
   */
  @GetMapping("/detail/{id}")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "获取提成记录详情")
  public Result<CommissionDTO> getCommission(@PathVariable final Long id) {
    CommissionDTO commission = commissionAppService.getCommission(id);
    return Result.success(commission);
  }

  /**
   * 查询用户提成总额
   *
   * @param userId 用户ID
   * @return 提成总额
   */
  @GetMapping("/users/{userId}/total")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "查询用户提成总额")
  public Result<BigDecimal> getUserTotalCommission(@PathVariable final Long userId) {
    BigDecimal total = commissionAppService.getUserTotalCommission(userId);
    return Result.success(total);
  }

  /**
   * 获取待计算提成的收款记录列表
   *
   * @return 待计算列表
   */
  @GetMapping("/pending-payments")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "获取待计算提成的收款记录列表", description = "返回所有已确认但还没有生成提成记录的收款记录")
  public Result<List<PaymentDTO>> getPendingCommissionPayments() {
    List<PaymentDTO> payments = commissionAppService.getPendingCommissionPayments();
    return Result.success(payments);
  }

  // ========== 提成汇总查看（M4-037，P1） ==========

  /**
   * 获取全所提成汇总
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 汇总数据
   */
  @GetMapping("/summary")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "获取全所提成汇总", description = "仅管理层可以查看")
  public Result<Map<String, Object>> getCommissionSummary(
      @RequestParam(required = false) final String startDate,
      @RequestParam(required = false) final String endDate) {
    Map<String, Object> summary = commissionAppService.getCommissionSummary(startDate, endDate);
    return Result.success(summary);
  }

  // ========== 提成审批（M4-038，P1） ==========

  /**
   * 审批提成
   *
   * @param id 提成ID
   * @param approved 是否通过
   * @param comment 审批意见
   * @return 审批后的提成
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "审批提成", description = "仅主任可以审批")
  @OperationLog(module = "提成管理", action = "审批提成")
  public Result<CommissionDTO> approveCommission(
      @PathVariable final Long id,
      @RequestParam final Boolean approved,
      @RequestParam(required = false) final String comment) {
    CommissionDTO commission = commissionAppService.approveCommission(id, approved, comment);
    return Result.success(commission);
  }

  /**
   * 批量审批提成
   *
   * @param ids 提成ID列表
   * @param approved 是否通过
   * @param comment 审批意见
   * @return 操作结果
   */
  @PostMapping("/batch-approve")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "批量审批提成", description = "仅主任可以审批")
  @OperationLog(module = "提成管理", action = "批量审批提成")
  public Result<Void> batchApproveCommissions(
      @RequestBody final List<Long> ids,
      @RequestParam final Boolean approved,
      @RequestParam(required = false) final String comment) {
    commissionAppService.batchApproveCommissions(ids, approved, comment);
    return Result.success();
  }

  // ========== 提成发放（M4-039，P1） ==========

  /**
   * 确认提成已发放
   *
   * @param id 提成ID
   * @return 操作结果
   */
  @PostMapping("/{id}/issue")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "确认提成已发放", description = "仅财务人员可以确认")
  @OperationLog(module = "提成管理", action = "确认提成发放")
  public Result<CommissionDTO> issueCommission(@PathVariable final Long id) {
    CommissionDTO commission = commissionAppService.issueCommission(id);
    return Result.success(commission);
  }

  /**
   * 批量确认提成发放
   *
   * @param ids 提成ID列表
   * @return 操作结果
   */
  @PostMapping("/batch-issue")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "批量确认提成发放", description = "仅财务人员可以确认")
  @OperationLog(module = "提成管理", action = "批量确认提成发放")
  public Result<Void> batchIssueCommissions(@RequestBody final List<Long> ids) {
    commissionAppService.batchIssueCommissions(ids);
    return Result.success();
  }

  // ========== 提成报表（M4-040，P1） ==========

  /**
   * 生成提成报表
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param userId 用户ID
   * @return 报表数据
   */
  @GetMapping("/report")
  @RequirePermission("finance:commission:manage")
  @Operation(summary = "生成提成报表", description = "财务和管理层可以查看")
  public Result<List<Map<String, Object>>> getCommissionReport(
      @RequestParam(required = false) final String startDate,
      @RequestParam(required = false) final String endDate,
      @RequestParam(required = false) final Long userId) {
    List<Map<String, Object>> report =
        commissionAppService.getCommissionReportData(startDate, endDate, userId);
    return Result.success(report);
  }
}
