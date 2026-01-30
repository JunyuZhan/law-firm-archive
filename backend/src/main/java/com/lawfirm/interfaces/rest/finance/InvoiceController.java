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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 发票管理 Controller */
@Tag(name = "发票管理", description = "发票管理相关接口")
@RestController
@RequestMapping("/finance/invoice")
@RequiredArgsConstructor
public class InvoiceController {

  /** 发票应用服务. */
  private final InvoiceAppService invoiceAppService;

  /**
   * 分页查询发票
   *
   * @param query 分页查询条件
   * @param clientId 客户ID
   * @param status 状态
   * @return 分页结果
   */
  @GetMapping("/list")
  @RequirePermission("finance:invoice:manage")
  public Result<PageResult<InvoiceDTO>> listInvoices(
      final PageQuery query,
      @RequestParam(required = false) final Long clientId,
      @RequestParam(required = false) final String status) {
    PageResult<InvoiceDTO> result = invoiceAppService.listInvoices(query, clientId, status);
    return Result.success(result);
  }

  /**
   * 获取发票详情
   *
   * @param id 发票ID
   * @return 发票详情
   */
  @GetMapping("/{id}")
  @RequirePermission("finance:invoice:manage")
  public Result<InvoiceDTO> getInvoice(@PathVariable final Long id) {
    InvoiceDTO invoice = invoiceAppService.getInvoiceById(id);
    return Result.success(invoice);
  }

  /**
   * 申请开票
   *
   * @param command 申请开票命令
   * @return 开票结果
   */
  @PostMapping("/apply")
  @RequirePermission("finance:invoice:manage")
  @OperationLog(module = "发票管理", action = "申请开票")
  public Result<InvoiceDTO> applyInvoice(@RequestBody @Valid final CreateInvoiceCommand command) {
    InvoiceDTO invoice = invoiceAppService.applyInvoice(command);
    return Result.success(invoice);
  }

  /**
   * 开票（确认开票）
   *
   * @param id 发票ID
   * @param request 开票请求
   * @return 操作结果
   */
  @PostMapping("/{id}/issue")
  @RequirePermission("finance:invoice:manage")
  @OperationLog(module = "发票管理", action = "开具发票")
  public Result<Void> issueInvoice(
      @PathVariable final Long id, @RequestBody final IssueRequest request) {
    invoiceAppService.issueInvoice(id, request.getInvoiceNo());
    return Result.success();
  }

  /**
   * 作废发票
   *
   * @param id 发票ID
   * @param request 作废请求
   * @return 操作结果
   */
  @PostMapping("/{id}/cancel")
  @RequirePermission("finance:invoice:manage")
  @OperationLog(module = "发票管理", action = "作废发票")
  public Result<Void> cancelInvoice(
      @PathVariable final Long id, @RequestBody final CancelRequest request) {
    invoiceAppService.cancelInvoice(id, request.getReason());
    return Result.success();
  }

  /**
   * 获取发票统计（M4-034）
   *
   * @return 发票统计结果
   */
  @GetMapping("/statistics")
  @RequirePermission("finance:invoice:manage")
  @Operation(summary = "获取发票统计", description = "统计开票金额，包括按客户、类型、状态、时间等维度")
  public Result<InvoiceStatisticsDTO> getInvoiceStatistics() {
    return Result.success(invoiceAppService.getInvoiceStatistics());
  }

  // ========== Request DTOs ==========

  /** 开票请求. */
  @Data
  public static class IssueRequest {
    /** 发票号码. */
    private String invoiceNo;
  }

  /** 作废请求. */
  @Data
  public static class CancelRequest {
    /** 作废原因. */
    private String reason;
  }
}
