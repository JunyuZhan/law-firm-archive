package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreatePaymentAmendmentCommand;
import com.lawfirm.application.finance.dto.PaymentAmendmentDTO;
import com.lawfirm.application.finance.service.PaymentAmendmentService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收款变更申请 Controller
 *
 * <p>Requirements: 3.5
 */
@RestController
@RequestMapping("/finance/payment-amendment")
@RequiredArgsConstructor
public class PaymentAmendmentController {

  /** 收款变更服务. */
  private final PaymentAmendmentService amendmentService;

  /**
   * 申请修改已锁定的收款记录
   *
   * @param command 创建命令
   * @return 变更申请详情
   */
  @PostMapping
  @RequirePermission("fee:amendment:create")
  @OperationLog(module = "收费管理", action = "申请收款变更")
  public Result<PaymentAmendmentDTO> requestAmendment(
      @RequestBody @Valid final CreatePaymentAmendmentCommand command) {
    PaymentAmendmentDTO result = amendmentService.requestAmendment(command);
    return Result.success(result);
  }

  /**
   * 审批通过变更申请
   *
   * @param id 变更申请ID
   * @param comment 审批意见
   * @return 变更申请详情
   */
  @PostMapping("/{id}/approve")
  @RequirePermission("fee:amendment:approve")
  @OperationLog(module = "收费管理", action = "审批通过收款变更")
  public Result<PaymentAmendmentDTO> approveAmendment(
      @PathVariable final Long id, @RequestParam(required = false) final String comment) {
    PaymentAmendmentDTO result = amendmentService.approveAmendment(id, comment);
    return Result.success(result);
  }

  /**
   * 拒绝变更申请
   *
   * @param id 变更申请ID
   * @param rejectReason 拒绝原因
   * @return 变更申请详情
   */
  @PostMapping("/{id}/reject")
  @RequirePermission("fee:amendment:approve")
  @OperationLog(module = "收费管理", action = "拒绝收款变更")
  public Result<PaymentAmendmentDTO> rejectAmendment(
      @PathVariable final Long id, @RequestParam final String rejectReason) {
    PaymentAmendmentDTO result = amendmentService.rejectAmendment(id, rejectReason);
    return Result.success(result);
  }

  /**
   * 分页查询变更申请列表
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @param status 状态
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("fee:amendment:list")
  public Result<PageResult<PaymentAmendmentDTO>> listAmendments(
      @RequestParam(defaultValue = "1") final int pageNum,
      @RequestParam(defaultValue = "10") final int pageSize,
      @RequestParam(required = false) final String status) {
    PageResult<PaymentAmendmentDTO> result =
        amendmentService.listAmendments(pageNum, pageSize, status);
    return Result.success(result);
  }

  /**
   * 获取变更申请详情
   *
   * @param id 变更申请ID
   * @return 变更申请详情
   */
  @GetMapping("/{id}")
  @RequirePermission("fee:amendment:list")
  public Result<PaymentAmendmentDTO> getAmendment(@PathVariable final Long id) {
    PaymentAmendmentDTO result = amendmentService.getAmendmentById(id);
    return Result.success(result);
  }

  /**
   * 查询收款记录的变更历史
   *
   * @param paymentId 收款ID
   * @return 变更历史列表
   */
  @GetMapping("/payment/{paymentId}/history")
  @RequirePermission("fee:amendment:list")
  public Result<List<PaymentAmendmentDTO>> getAmendmentHistory(@PathVariable final Long paymentId) {
    List<PaymentAmendmentDTO> result = amendmentService.getAmendmentsByPaymentId(paymentId);
    return Result.success(result);
  }
}
