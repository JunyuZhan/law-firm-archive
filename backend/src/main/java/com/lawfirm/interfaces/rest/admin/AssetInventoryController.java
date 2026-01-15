package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreateAssetInventoryCommand;
import com.lawfirm.application.admin.dto.AssetInventoryDTO;
import com.lawfirm.application.admin.service.AssetInventoryAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产盘点接口（M8-033）
 */
@Tag(name = "资产盘点", description = "资产盘点管理")
@RestController
@RequestMapping("/admin/asset-inventories")
@RequiredArgsConstructor
public class AssetInventoryController {

    private final AssetInventoryAppService inventoryAppService;

    @Operation(summary = "分页查询资产盘点列表")
    @GetMapping
    @RequirePermission("admin:asset:inventory")
    public Result<PageResult<AssetInventoryDTO>> list(
            PageQuery query,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        return Result.success(inventoryAppService.listInventories(query, status));
    }

    @Operation(summary = "创建资产盘点")
    @PostMapping
    @RequirePermission("admin:asset:inventory")
    @OperationLog(module = "资产盘点", action = "创建资产盘点")
    public Result<AssetInventoryDTO> createInventory(@RequestBody @Valid CreateAssetInventoryCommand command) {
        return Result.success(inventoryAppService.createInventory(command));
    }

    @Operation(summary = "更新盘点明细")
    @PutMapping("/details/{detailId}")
    @RequirePermission("admin:asset:inventory")
    @OperationLog(module = "资产盘点", action = "更新盘点明细")
    public Result<Void> updateDetail(@PathVariable Long detailId, @RequestBody UpdateDetailRequest request) {
        inventoryAppService.updateInventoryDetail(
                detailId, request.getActualStatus(), request.getActualLocation(),
                request.getActualUserId(), request.getDiscrepancyDesc());
        return Result.success();
    }

    @Operation(summary = "完成盘点")
    @PostMapping("/{id}/complete")
    @RequirePermission("admin:asset:inventory")
    @OperationLog(module = "资产盘点", action = "完成盘点")
    public Result<AssetInventoryDTO> completeInventory(@PathVariable Long id) {
        return Result.success(inventoryAppService.completeInventory(id));
    }

    @Operation(summary = "获取盘点详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:asset:inventory")
    public Result<AssetInventoryDTO> getInventory(@PathVariable Long id) {
        return Result.success(inventoryAppService.getInventoryById(id));
    }

    @Operation(summary = "查询进行中的盘点")
    @GetMapping("/in-progress")
    @RequirePermission("admin:asset:inventory")
    public Result<List<AssetInventoryDTO>> getInProgressInventories() {
        return Result.success(inventoryAppService.getInProgressInventories());
    }

    @Data
    public static class UpdateDetailRequest {
        private String actualStatus;
        private String actualLocation;
        private Long actualUserId;
        private String discrepancyDesc;
    }
}

