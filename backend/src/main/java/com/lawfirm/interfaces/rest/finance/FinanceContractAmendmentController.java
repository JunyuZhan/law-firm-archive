package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.service.FinanceContractAmendmentService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.finance.entity.FinanceContractAmendment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务合同变更管理控制器
 *
 * <p>用于财务人员处理律师变更合同后的数据同步
 */
@Tag(name = "财务合同变更管理")
@RestController
@RequestMapping("/finance/contract-amendments")
@RequiredArgsConstructor
public class FinanceContractAmendmentController {

  /** 财务合同变更服务. */
  private final FinanceContractAmendmentService amendmentService;

  /**
   * 查询所有变更记录
   *
   * @return 变更记录列表
   */
  @Operation(summary = "查询所有变更记录")
  @GetMapping
  @RequirePermission("finance:contract:amendment:view")
  public Result<List<FinanceContractAmendment>> getAllAmendments() {
    // 返回待处理的变更记录作为默认列表
    List<FinanceContractAmendment> amendments = amendmentService.getPendingAmendments();
    return Result.success(amendments);
  }

  /**
   * 查询待处理的变更记录
   *
   * @return 待处理变更记录列表
   */
  @Operation(summary = "查询待处理的变更记录")
  @GetMapping("/pending")
  @RequirePermission("finance:contract:amendment:view")
  public Result<List<FinanceContractAmendment>> getPendingAmendments() {
    List<FinanceContractAmendment> amendments = amendmentService.getPendingAmendments();
    return Result.success(amendments);
  }

  /**
   * 查询合同的变更记录
   *
   * @param contractId 合同ID
   * @return 变更记录列表
   */
  @Operation(summary = "查询合同的变更记录")
  @GetMapping("/contract/{contractId}")
  @RequirePermission("finance:contract:amendment:view")
  public Result<List<FinanceContractAmendment>> getAmendmentsByContractId(
      @PathVariable final Long contractId) {
    List<FinanceContractAmendment> amendments =
        amendmentService.getAmendmentsByContractId(contractId);
    return Result.success(amendments);
  }

  /**
   * 同步变更到财务数据
   *
   * @param amendmentId 变更记录ID
   * @param remark 备注
   * @return 操作结果
   */
  @Operation(summary = "同步变更到财务数据")
  @PostMapping("/{amendmentId}/sync")
  @RequirePermission("finance:contract:amendment:sync")
  public Result<Void> syncAmendment(
      @PathVariable final Long amendmentId, @RequestParam(required = false) final String remark) {
    amendmentService.syncAmendment(amendmentId, remark);
    return Result.success();
  }

  /**
   * 忽略变更（不同步）
   *
   * @param amendmentId 变更记录ID
   * @param remark 备注
   * @return 操作结果
   */
  @Operation(summary = "忽略变更（不同步）")
  @PostMapping("/{amendmentId}/ignore")
  @RequirePermission("finance:contract:amendment:sync")
  public Result<Void> ignoreAmendment(
      @PathVariable final Long amendmentId, @RequestParam final String remark) {
    amendmentService.ignoreAmendment(amendmentId, remark);
    return Result.success();
  }
}
