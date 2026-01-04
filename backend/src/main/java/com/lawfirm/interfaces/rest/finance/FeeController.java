package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreateFeeCommand;
import com.lawfirm.application.finance.command.CreatePaymentCommand;
import com.lawfirm.application.finance.dto.FeeDTO;
import com.lawfirm.application.finance.dto.FeeQueryDTO;
import com.lawfirm.application.finance.dto.PaymentDTO;
import com.lawfirm.application.finance.dto.ReconciliationResultDTO;
import com.lawfirm.application.finance.service.FeeAppService;
import com.lawfirm.application.finance.service.PaymentReconciliationService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 收费管理 Controller
 */
@RestController
@RequestMapping("/finance/fee")
@RequiredArgsConstructor
public class FeeController {

    private final FeeAppService feeAppService;
    private final PaymentReconciliationService reconciliationService;

    /**
     * 分页查询收费记录
     */
    @GetMapping("/list")
    @RequirePermission("fee:list")
    public Result<PageResult<FeeDTO>> listFees(FeeQueryDTO query) {
        PageResult<FeeDTO> result = feeAppService.listFees(query);
        return Result.success(result);
    }

    /**
     * 获取收费详情
     */
    @GetMapping("/{id}")
    @RequirePermission("fee:list")
    public Result<FeeDTO> getFee(@PathVariable Long id) {
        FeeDTO fee = feeAppService.getFeeById(id);
        return Result.success(fee);
    }

    /**
     * 创建收费记录
     */
    @PostMapping
    @RequirePermission("fee:create")
    @OperationLog(module = "收费管理", action = "创建收费记录")
    public Result<FeeDTO> createFee(@RequestBody @Valid CreateFeeCommand command) {
        FeeDTO fee = feeAppService.createFee(command);
        return Result.success(fee);
    }

    /**
     * 创建收款记录
     */
    @PostMapping("/payment")
    @RequirePermission("fee:payment")
    @OperationLog(module = "收费管理", action = "创建收款记录")
    public Result<PaymentDTO> createPayment(@RequestBody @Valid CreatePaymentCommand command) {
        PaymentDTO payment = feeAppService.createPayment(command);
        return Result.success(payment);
    }

    /**
     * 确认收款
     */
    @PostMapping("/payment/{id}/confirm")
    @RequirePermission("fee:confirm")
    @OperationLog(module = "收费管理", action = "确认收款")
    public Result<Void> confirmPayment(@PathVariable Long id) {
        feeAppService.confirmPayment(id);
        return Result.success();
    }

    /**
     * 取消收款
     */
    @PostMapping("/payment/{id}/cancel")
    @RequirePermission("fee:payment")
    @OperationLog(module = "收费管理", action = "取消收款")
    public Result<Void> cancelPayment(@PathVariable Long id) {
        feeAppService.cancelPayment(id);
        return Result.success();
    }

    /**
     * 智能匹配收款
     * 根据收款金额、付款方名称等信息智能匹配待收款记录
     */
    @GetMapping("/reconciliation/match")
    @RequirePermission("fee:payment")
    public Result<ReconciliationResultDTO> intelligentMatch(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String payerName,
            @RequestParam(required = false) String transactionNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate) {
        ReconciliationResultDTO result = reconciliationService.intelligentMatch(
                amount, payerName, transactionNo, paymentDate);
        return Result.success(result);
    }
}

