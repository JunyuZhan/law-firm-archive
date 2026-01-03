package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreatePurchaseRequestCommand;
import com.lawfirm.application.admin.command.PurchaseReceiveCommand;
import com.lawfirm.application.admin.dto.PurchaseReceiveDTO;
import com.lawfirm.application.admin.dto.PurchaseRequestDTO;
import com.lawfirm.application.admin.service.PurchaseAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 采购管理接口
 */
@Tag(name = "采购管理", description = "采购申请、审批、入库相关接口")
@RestController
@RequestMapping("/admin/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseAppService purchaseAppService;

    @Operation(summary = "分页查询采购申请")
    @GetMapping
    @RequirePermission("admin:purchase:list")
    public Result<PageResult<PurchaseRequestDTO>> list(
            PageQuery query,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "采购类型") @RequestParam(required = false) String purchaseType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "申请人ID") @RequestParam(required = false) Long applicantId,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId) {
        return Result.success(purchaseAppService.listRequests(query, keyword, purchaseType, status, applicantId, departmentId));
    }

    @Operation(summary = "获取采购申请详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:purchase:detail")
    public Result<PurchaseRequestDTO> getById(@PathVariable Long id) {
        return Result.success(purchaseAppService.getRequestById(id));
    }

    @Operation(summary = "创建采购申请")
    @PostMapping
    @RequirePermission("admin:purchase:create")
    @OperationLog(module = "采购管理", action = "创建采购申请")
    public Result<PurchaseRequestDTO> create(@RequestBody CreatePurchaseRequestCommand command) {
        return Result.success(purchaseAppService.createRequest(command));
    }

    @Operation(summary = "提交采购申请")
    @PostMapping("/{id}/submit")
    @RequirePermission("admin:purchase:create")
    @OperationLog(module = "采购管理", action = "提交采购申请")
    public Result<Void> submit(@PathVariable Long id) {
        purchaseAppService.submitRequest(id);
        return Result.success();
    }

    @Operation(summary = "审批采购申请")
    @PostMapping("/{id}/approve")
    @RequirePermission("admin:purchase:approve")
    @OperationLog(module = "采购管理", action = "审批采购申请")
    public Result<Void> approve(
            @PathVariable Long id,
            @Parameter(description = "是否批准") @RequestParam boolean approved,
            @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        purchaseAppService.approveRequest(id, approved, comment);
        return Result.success();
    }


    @Operation(summary = "开始采购")
    @PostMapping("/{id}/start")
    @RequirePermission("admin:purchase:edit")
    @OperationLog(module = "采购管理", action = "开始采购")
    public Result<Void> startPurchasing(
            @PathVariable Long id,
            @Parameter(description = "供应商ID") @RequestParam Long supplierId) {
        purchaseAppService.startPurchasing(id, supplierId);
        return Result.success();
    }

    @Operation(summary = "采购入库")
    @PostMapping("/receive")
    @RequirePermission("admin:purchase:receive")
    @OperationLog(module = "采购管理", action = "采购入库")
    public Result<PurchaseReceiveDTO> receive(@RequestBody PurchaseReceiveCommand command) {
        return Result.success(purchaseAppService.receiveItem(command));
    }

    @Operation(summary = "取消采购申请")
    @PostMapping("/{id}/cancel")
    @RequirePermission("admin:purchase:edit")
    @OperationLog(module = "采购管理", action = "取消采购申请")
    public Result<Void> cancel(@PathVariable Long id) {
        purchaseAppService.cancelRequest(id);
        return Result.success();
    }

    @Operation(summary = "获取入库记录")
    @GetMapping("/{id}/receives")
    @RequirePermission("admin:purchase:detail")
    public Result<List<PurchaseReceiveDTO>> getReceiveRecords(@PathVariable Long id) {
        return Result.success(purchaseAppService.getReceiveRecords(id));
    }

    @Operation(summary = "获取我的采购申请")
    @GetMapping("/my")
    public Result<List<PurchaseRequestDTO>> getMyRequests() {
        return Result.success(purchaseAppService.getMyRequests());
    }

    @Operation(summary = "获取待审批的采购申请")
    @GetMapping("/pending")
    @RequirePermission("admin:purchase:approve")
    public Result<List<PurchaseRequestDTO>> getPendingApproval() {
        return Result.success(purchaseAppService.getPendingApproval());
    }

    @Operation(summary = "获取采购统计")
    @GetMapping("/statistics")
    @RequirePermission("admin:purchase:list")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(purchaseAppService.getStatistics());
    }
}
