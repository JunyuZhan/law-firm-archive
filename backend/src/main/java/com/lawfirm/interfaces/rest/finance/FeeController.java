package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreateFeeCommand;
import com.lawfirm.application.finance.command.CreatePaymentCommand;
import com.lawfirm.application.finance.dto.FeeDTO;
import com.lawfirm.application.finance.dto.FeeQueryDTO;
import com.lawfirm.application.finance.dto.PaymentDTO;
import com.lawfirm.application.finance.dto.ReconciliationResultDTO;
import com.lawfirm.application.finance.service.FeeAppService;
import com.lawfirm.application.finance.service.PaymentReconciliationService;
import com.lawfirm.common.annotation.Idempotent;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RepeatSubmit;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 收费管理 Controller */
@RestController
@RequestMapping("/finance/fee")
@RequiredArgsConstructor
public class FeeController {

  /** 收费应用服务. */
  private final FeeAppService feeAppService;

  /** 支付对账服务. */
  private final PaymentReconciliationService reconciliationService;

  /**
   * 分页查询收费记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("finance:payment:manage")
  public Result<PageResult<FeeDTO>> listFees(final FeeQueryDTO query) {
    PageResult<FeeDTO> result = feeAppService.listFees(query);
    return Result.success(result);
  }

  /**
   * 获取收费详情
   *
   * @param id 收费ID
   * @return 收费详情
   */
  @GetMapping("/{id}")
  @RequirePermission("finance:payment:manage")
  public Result<FeeDTO> getFee(@PathVariable final Long id) {
    FeeDTO fee = feeAppService.getFeeById(id);
    return Result.success(fee);
  }

  /**
   * 创建收费记录
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping
  @RequirePermission("finance:payment:manage")
  @OperationLog(module = "收费管理", action = "创建收费记录")
  @RepeatSubmit(interval = 5000, message = "请勿重复提交收费记录")
  public Result<FeeDTO> createFee(@RequestBody @Valid final CreateFeeCommand command) {
    FeeDTO fee = feeAppService.createFee(command);
    return Result.success(fee);
  }

  /**
   * 创建收款记录
   *
   * @param command 创建命令
   * @return 创建结果
   */
  @PostMapping("/payment")
  @RequirePermission("finance:payment:manage")
  @OperationLog(module = "收费管理", action = "创建收款记录")
  @RepeatSubmit(interval = 5000, message = "请勿重复提交收款记录")
  public Result<PaymentDTO> createPayment(@RequestBody @Valid final CreatePaymentCommand command) {
    PaymentDTO payment = feeAppService.createPayment(command);
    return Result.success(payment);
  }

  /**
   * 确认收款
   *
   * @param id 收款ID
   * @return 操作结果
   */
  @PostMapping("/payment/{id}/confirm")
  @RequirePermission("finance:payment:manage")
  @OperationLog(module = "收费管理", action = "确认收款")
  @Idempotent(key = "#id", expireSeconds = 3600, message = "该收款已确认，请勿重复操作")
  public Result<Void> confirmPayment(@PathVariable final Long id) {
    feeAppService.confirmPayment(id);
    return Result.success();
  }

  /**
   * 取消收款
   *
   * @param id 收款ID
   * @return 操作结果
   */
  @PostMapping("/payment/{id}/cancel")
  @RequirePermission("finance:payment:manage")
  @OperationLog(module = "收费管理", action = "取消收款")
  @Idempotent(key = "#id", expireSeconds = 3600, message = "该收款已取消，请勿重复操作")
  public Result<Void> cancelPayment(@PathVariable final Long id) {
    feeAppService.cancelPayment(id);
    return Result.success();
  }

  /**
   * 智能匹配收款 根据收款金额、付款方名称等信息智能匹配待收款记录
   *
   * @param amount 收款金额
   * @param payerName 付款方名称
   * @param transactionNo 交易流水号
   * @param paymentDate 支付日期
   * @return 匹配结果
   */
  @GetMapping("/reconciliation/match")
  @RequirePermission("finance:payment:manage")
  public Result<ReconciliationResultDTO> intelligentMatch(
      @RequestParam final BigDecimal amount,
      @RequestParam(required = false) final String payerName,
      @RequestParam(required = false) final String transactionNo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          final LocalDate paymentDate) {
    ReconciliationResultDTO result =
        reconciliationService.intelligentMatch(amount, payerName, transactionNo, paymentDate);
    return Result.success(result);
  }
}
