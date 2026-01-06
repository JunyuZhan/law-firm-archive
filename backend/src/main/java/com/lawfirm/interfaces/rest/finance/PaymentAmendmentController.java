package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreatePaymentAmendmentCommand;
import com.lawfirm.application.finance.dto.PaymentAmendmentDTO;
import com.lawfirm.application.finance.service.PaymentAmendmentService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收款变更申请 Controller
 * 
 * Requirements: 3.5
 */
@RestController
@RequestMapping("/finance/payment-amendment")
@RequiredArgsConstructor
public class PaymentAmendmentController {

    private final PaymentAmendmentService amendmentService;

    /**
     * 申请修改已锁定的收款记录
     */
    @PostMapping
    @RequirePermission("fee:amendment:create")
    @OperationLog(module = "收费管理", action = "申请收款变更")
    public Result<PaymentAmendmentDTO> requestAmendment(@RequestBody @Valid CreatePaymentAmendmentCommand command) {
        PaymentAmendmentDTO result = amendmentService.requestAmendment(command);
        return Result.success(result);
    }

    /**
     * 审批通过变更申请
     */
    @PostMapping("/{id}/approve")
    @RequirePermission("fee:amendment:approve")
    @OperationLog(module = "收费管理", action = "审批通过收款变更")
    public Result<PaymentAmendmentDTO> approveAmendment(
            @PathVariable Long id,
            @RequestParam(required = false) String comment) {
        PaymentAmendmentDTO result = amendmentService.approveAmendment(id, comment);
        return Result.success(result);
    }

    /**
     * 拒绝变更申请
     */
    @PostMapping("/{id}/reject")
    @RequirePermission("fee:amendment:approve")
    @OperationLog(module = "收费管理", action = "拒绝收款变更")
    public Result<PaymentAmendmentDTO> rejectAmendment(
            @PathVariable Long id,
            @RequestParam String rejectReason) {
        PaymentAmendmentDTO result = amendmentService.rejectAmendment(id, rejectReason);
        return Result.success(result);
    }

    /**
     * 分页查询变更申请列表
     */
    @GetMapping("/list")
    @RequirePermission("fee:amendment:list")
    public Result<PageResult<PaymentAmendmentDTO>> listAmendments(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        PageResult<PaymentAmendmentDTO> result = amendmentService.listAmendments(pageNum, pageSize, status);
        return Result.success(result);
    }

    /**
     * 获取变更申请详情
     */
    @GetMapping("/{id}")
    @RequirePermission("fee:amendment:list")
    public Result<PaymentAmendmentDTO> getAmendment(@PathVariable Long id) {
        PaymentAmendmentDTO result = amendmentService.getAmendmentById(id);
        return Result.success(result);
    }

    /**
     * 查询收款记录的变更历史
     */
    @GetMapping("/payment/{paymentId}/history")
    @RequirePermission("fee:amendment:list")
    public Result<List<PaymentAmendmentDTO>> getAmendmentHistory(@PathVariable Long paymentId) {
        List<PaymentAmendmentDTO> result = amendmentService.getAmendmentsByPaymentId(paymentId);
        return Result.success(result);
    }
}
