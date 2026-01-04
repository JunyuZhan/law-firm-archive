package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreatePrepaymentCommand;
import com.lawfirm.application.finance.command.UsePrepaymentCommand;
import com.lawfirm.application.finance.dto.PrepaymentDTO;
import com.lawfirm.application.finance.dto.PrepaymentQueryDTO;
import com.lawfirm.application.finance.dto.PrepaymentUsageDTO;
import com.lawfirm.application.finance.service.PrepaymentAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预收款管理 Controller
 */
@RestController
@RequestMapping("/finance/prepayment")
@RequiredArgsConstructor
public class PrepaymentController {

    private final PrepaymentAppService prepaymentAppService;

    /**
     * 分页查询预收款
     */
    @GetMapping("/list")
    @RequirePermission("prepayment:list")
    public Result<PageResult<PrepaymentDTO>> listPrepayments(PrepaymentQueryDTO query) {
        PageResult<PrepaymentDTO> result = prepaymentAppService.listPrepayments(query);
        return Result.success(result);
    }

    /**
     * 获取预收款详情
     */
    @GetMapping("/{id}")
    @RequirePermission("prepayment:list")
    public Result<PrepaymentDTO> getPrepayment(@PathVariable Long id) {
        PrepaymentDTO prepayment = prepaymentAppService.getPrepaymentById(id);
        return Result.success(prepayment);
    }

    /**
     * 创建预收款
     */
    @PostMapping
    @RequirePermission("prepayment:create")
    @OperationLog(module = "预收款管理", action = "创建预收款")
    public Result<PrepaymentDTO> createPrepayment(@RequestBody @Valid CreatePrepaymentCommand command) {
        PrepaymentDTO prepayment = prepaymentAppService.createPrepayment(command);
        return Result.success(prepayment);
    }

    /**
     * 确认预收款
     */
    @PostMapping("/{id}/confirm")
    @RequirePermission("prepayment:confirm")
    @OperationLog(module = "预收款管理", action = "确认预收款")
    public Result<PrepaymentDTO> confirmPrepayment(@PathVariable Long id) {
        PrepaymentDTO prepayment = prepaymentAppService.confirmPrepayment(id);
        return Result.success(prepayment);
    }

    /**
     * 使用预收款（核销）
     */
    @PostMapping("/use")
    @RequirePermission("prepayment:use")
    @OperationLog(module = "预收款管理", action = "使用预收款")
    public Result<PrepaymentUsageDTO> usePrepayment(@RequestBody @Valid UsePrepaymentCommand command) {
        PrepaymentUsageDTO usage = prepaymentAppService.usePrepayment(command);
        return Result.success(usage);
    }

    /**
     * 查询客户可用预收款
     */
    @GetMapping("/available/{clientId}")
    @RequirePermission("prepayment:list")
    public Result<List<PrepaymentDTO>> getAvailablePrepayments(@PathVariable Long clientId) {
        List<PrepaymentDTO> prepayments = prepaymentAppService.getAvailablePrepayments(clientId);
        return Result.success(prepayments);
    }

    /**
     * 退款
     */
    @PostMapping("/{id}/refund")
    @RequirePermission("prepayment:refund")
    @OperationLog(module = "预收款管理", action = "预收款退款")
    public Result<PrepaymentDTO> refundPrepayment(@PathVariable Long id, @RequestParam String remark) {
        PrepaymentDTO prepayment = prepaymentAppService.refundPrepayment(id, remark);
        return Result.success(prepayment);
    }
}
