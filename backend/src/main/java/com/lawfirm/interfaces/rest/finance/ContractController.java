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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 合同管理 Controller */
@RestController("financeContractController")
@RequestMapping("/finance/contract")
@RequiredArgsConstructor
public class ContractController {

  /** 合同应用服务. */
  private final ContractAppService contractAppService;

  /**
   * 分页查询合同列表（财务模块：只读）
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("finance:contract:view")
  public Result<PageResult<ContractDTO>> listContracts(final ContractQueryDTO query) {
    PageResult<ContractDTO> result = contractAppService.listContracts(query);
    return Result.success(result);
  }

  /**
   * 获取合同详情（财务模块：只读）
   *
   * @param id 合同ID
   * @return 合同详情
   */
  @GetMapping("/{id}")
  @RequirePermission("finance:contract:view")
  public Result<ContractDTO> getContract(@PathVariable final Long id) {
    ContractDTO contract = contractAppService.getContractById(id);
    return Result.success(contract);
  }

  /**
   * 提交审批（保留此接口，供项目管理模块调用）
   *
   * @param id 合同ID
   * @param approverId 审批人ID
   * @return 操作结果
   */
  @PostMapping("/{id}/submit")
  @RequirePermission("matter:contract:submit")
  @OperationLog(module = "合同管理", action = "提交审批")
  public Result<Void> submitForApproval(
      @PathVariable final Long id, @RequestParam(required = false) final Long approverId) {
    contractAppService.submitForApproval(id, approverId);
    return Result.success();
  }

  /**
   * 审批通过（保留此接口，供审批中心调用）
   *
   * @param id 合同ID
   * @return 操作结果
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("contract:approve")
  @OperationLog(module = "合同管理", action = "审批通过")
  public Result<Void> approve(@PathVariable final Long id) {
    contractAppService.approve(id);
    return Result.success();
  }

  /**
   * 审批拒绝（保留此接口，供审批中心调用）
   *
   * @param id 合同ID
   * @param request 拒绝原因
   * @return 操作结果
   */
  @PostMapping("/{id}/reject")
  @RequirePermission("contract:approve")
  @OperationLog(module = "合同管理", action = "审批拒绝")
  public Result<Void> reject(
      @PathVariable final Long id, @RequestBody final RejectRequest request) {
    contractAppService.reject(id, request.getReason());
    return Result.success();
  }

  /**
   * 撤回审批（合同创建者或签约人撤回待审批的合同）
   *
   * @param id 合同ID
   * @return 操作结果
   */
  @PostMapping("/{id}/withdraw")
  @RequirePermission("matter:contract:submit")
  @OperationLog(module = "合同管理", action = "撤回审批")
  public Result<Void> withdrawApproval(@PathVariable final Long id) {
    contractAppService.withdrawApproval(id);
    return Result.success();
  }

  // ========== Request DTOs ==========

  /** 拒绝请求. */
  @Data
  public static class RejectRequest {
    /** 拒绝原因. */
    private String reason;
  }
}
