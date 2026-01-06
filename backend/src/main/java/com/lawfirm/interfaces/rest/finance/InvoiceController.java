package com.lawfirm.interfaces.rest.finance;

import com.lawfirm.application.finance.command.CreateInvoiceCommand;
import com.lawfirm.application.finance.dto.InvoiceDTO;
import com.lawfirm.application.finance.dto.InvoiceStatisticsDTO;
import com.lawfirm.application.finance.service.InvoiceAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 发票管理 Controller
 */
@Tag(name = "发票管理", description = "发票管理相关接口")
@RestController
@RequestMapping("/finance/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceAppService invoiceAppService;

    /**
     * 分页查询发票
     */
    @GetMapping("/list")
    @RequirePermission("finance:invoice:manage")
    public Result<PageResult<InvoiceDTO>> listInvoices(PageQuery query,
                                                       @RequestParam(required = false) Long clientId,
                                                       @RequestParam(required = false) String status) {
        PageResult<InvoiceDTO> result = invoiceAppService.listInvoices(query, clientId, status);
        return Result.success(result);
    }

    /**
     * 获取发票详情
     */
    @GetMapping("/{id}")
    @RequirePermission("finance:invoice:manage")
    public Result<InvoiceDTO> getInvoice(@PathVariable Long id) {
        InvoiceDTO invoice = invoiceAppService.getInvoiceById(id);
        return Result.success(invoice);
    }

    /**
     * 申请开票
     */
    @PostMapping("/apply")
    @RequirePermission("finance:invoice:manage")
    @OperationLog(module = "发票管理", action = "申请开票")
    public Result<InvoiceDTO> applyInvoice(@RequestBody @Valid CreateInvoiceCommand command) {
        InvoiceDTO invoice = invoiceAppService.applyInvoice(command);
        return Result.success(invoice);
    }

    /**
     * 开票（确认开票）
     */
    @PostMapping("/{id}/issue")
    @RequirePermission("finance:invoice:manage")
    @OperationLog(module = "发票管理", action = "开具发票")
    public Result<Void> issueInvoice(@PathVariable Long id, @RequestBody IssueRequest request) {
        invoiceAppService.issueInvoice(id, request.getInvoiceNo());
        return Result.success();
    }

    /**
     * 作废发票
     */
    @PostMapping("/{id}/cancel")
    @RequirePermission("finance:invoice:manage")
    @OperationLog(module = "发票管理", action = "作废发票")
    public Result<Void> cancelInvoice(@PathVariable Long id, @RequestBody CancelRequest request) {
        invoiceAppService.cancelInvoice(id, request.getReason());
        return Result.success();
    }

    /**
     * 获取发票统计（M4-034）
     */
    @GetMapping("/statistics")
    @RequirePermission("finance:invoice:manage")
    @Operation(summary = "获取发票统计", description = "统计开票金额，包括按客户、类型、状态、时间等维度")
    public Result<InvoiceStatisticsDTO> getInvoiceStatistics() {
        return Result.success(invoiceAppService.getInvoiceStatistics());
    }

    // ========== Request DTOs ==========

    @Data
    public static class IssueRequest {
        private String invoiceNo;
    }

    @Data
    public static class CancelRequest {
        private String reason;
    }
}

