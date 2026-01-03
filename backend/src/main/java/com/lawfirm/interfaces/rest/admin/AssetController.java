package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.AssetReceiveCommand;
import com.lawfirm.application.admin.command.CreateAssetCommand;
import com.lawfirm.application.admin.dto.AssetDTO;
import com.lawfirm.application.admin.dto.AssetRecordDTO;
import com.lawfirm.application.admin.service.AssetAppService;
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
 * 资产管理接口
 */
@Tag(name = "资产管理", description = "固定资产管理相关接口")
@RestController
@RequestMapping("/admin/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetAppService assetAppService;

    @Operation(summary = "分页查询资产列表")
    @GetMapping
    @RequirePermission("admin:asset:list")
    public Result<PageResult<AssetDTO>> list(
            PageQuery query,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "资产类别") @RequestParam(required = false) String category,
            @Parameter(description = "资产状态") @RequestParam(required = false) String status,
            @Parameter(description = "所属部门ID") @RequestParam(required = false) Long departmentId) {
        return Result.success(assetAppService.listAssets(query, keyword, category, status, departmentId));
    }

    @Operation(summary = "获取资产详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:asset:detail")
    public Result<AssetDTO> getById(@PathVariable Long id) {
        return Result.success(assetAppService.getAssetById(id));
    }

    @Operation(summary = "创建资产")
    @PostMapping
    @RequirePermission("admin:asset:create")
    @OperationLog(module = "资产管理", action = "创建资产")
    public Result<AssetDTO> create(@RequestBody CreateAssetCommand command) {
        return Result.success(assetAppService.createAsset(command));
    }

    @Operation(summary = "更新资产")
    @PutMapping("/{id}")
    @RequirePermission("admin:asset:edit")
    @OperationLog(module = "资产管理", action = "更新资产")
    public Result<AssetDTO> update(@PathVariable Long id, @RequestBody CreateAssetCommand command) {
        return Result.success(assetAppService.updateAsset(id, command));
    }

    @Operation(summary = "删除资产")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:asset:delete")
    @OperationLog(module = "资产管理", action = "删除资产")
    public Result<Void> delete(@PathVariable Long id) {
        assetAppService.deleteAsset(id);
        return Result.success();
    }


    @Operation(summary = "资产领用")
    @PostMapping("/receive")
    @RequirePermission("admin:asset:receive")
    @OperationLog(module = "资产管理", action = "资产领用")
    public Result<Void> receive(@RequestBody AssetReceiveCommand command) {
        assetAppService.receiveAsset(command);
        return Result.success();
    }

    @Operation(summary = "资产归还")
    @PostMapping("/{id}/return")
    @RequirePermission("admin:asset:return")
    @OperationLog(module = "资产管理", action = "资产归还")
    public Result<Void> returnAsset(
            @PathVariable Long id,
            @Parameter(description = "备注") @RequestParam(required = false) String remarks) {
        assetAppService.returnAsset(id, remarks);
        return Result.success();
    }

    @Operation(summary = "资产报废")
    @PostMapping("/{id}/scrap")
    @RequirePermission("admin:asset:scrap")
    @OperationLog(module = "资产管理", action = "资产报废")
    public Result<Void> scrap(
            @PathVariable Long id,
            @Parameter(description = "报废原因") @RequestParam String reason) {
        assetAppService.scrapAsset(id, reason);
        return Result.success();
    }

    @Operation(summary = "获取资产操作记录")
    @GetMapping("/{id}/records")
    @RequirePermission("admin:asset:detail")
    public Result<List<AssetRecordDTO>> getRecords(@PathVariable Long id) {
        return Result.success(assetAppService.getAssetRecords(id));
    }

    @Operation(summary = "获取我领用的资产")
    @GetMapping("/my")
    public Result<List<AssetDTO>> getMyAssets() {
        return Result.success(assetAppService.getMyAssets());
    }

    @Operation(summary = "获取闲置资产")
    @GetMapping("/idle")
    @RequirePermission("admin:asset:list")
    public Result<List<AssetDTO>> getIdleAssets() {
        return Result.success(assetAppService.getIdleAssets());
    }

    @Operation(summary = "获取资产统计")
    @GetMapping("/statistics")
    @RequirePermission("admin:asset:list")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(assetAppService.getAssetStatistics());
    }
}
