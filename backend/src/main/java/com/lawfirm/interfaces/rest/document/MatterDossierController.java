package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.service.MatterDossierService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.document.entity.DossierTemplate;
import com.lawfirm.domain.document.entity.DossierTemplateItem;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目卷宗管理接口
 */
@Tag(name = "项目卷宗管理")
@RestController
@RequestMapping("/matter/{matterId}/dossier")
@RequiredArgsConstructor
public class MatterDossierController {

    private final MatterDossierService dossierService;

    /**
     * 获取项目卷宗目录
     */
    @Operation(summary = "获取项目卷宗目录")
    @GetMapping
    @RequirePermission("doc:list")
    public Result<List<MatterDossierItem>> getDossierItems(@PathVariable Long matterId) {
        return Result.success(dossierService.getDossierItems(matterId));
    }

    /**
     * 初始化项目卷宗目录
     */
    @Operation(summary = "初始化项目卷宗目录")
    @PostMapping("/init")
    @RequirePermission("doc:create")
    @OperationLog(module = "卷宗管理", action = "初始化卷宗目录")
    public Result<List<MatterDossierItem>> initializeDossier(@PathVariable Long matterId) {
        dossierService.initializeDossier(matterId);
        return Result.success(dossierService.getDossierItems(matterId));
    }

    /**
     * 添加自定义目录项
     */
    @Operation(summary = "添加自定义目录项")
    @PostMapping("/item")
    @RequirePermission("doc:create")
    @OperationLog(module = "卷宗管理", action = "添加目录项")
    public Result<MatterDossierItem> addDossierItem(
            @PathVariable Long matterId,
            @RequestBody Map<String, Object> params) {
        Long parentId = params.get("parentId") != null ? 
                Long.valueOf(params.get("parentId").toString()) : 0L;
        String name = (String) params.get("name");
        String itemType = (String) params.get("itemType");
        
        return Result.success(dossierService.addDossierItem(matterId, parentId, name, itemType));
    }

    /**
     * 更新目录项
     */
    @Operation(summary = "更新目录项")
    @PutMapping("/item/{itemId}")
    @RequirePermission("doc:edit")
    @OperationLog(module = "卷宗管理", action = "更新目录项")
    public Result<MatterDossierItem> updateDossierItem(
            @PathVariable Long matterId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> params) {
        String name = (String) params.get("name");
        Integer sortOrder = params.get("sortOrder") != null ? 
                Integer.valueOf(params.get("sortOrder").toString()) : null;
        
        return Result.success(dossierService.updateDossierItem(itemId, name, sortOrder));
    }

    /**
     * 删除目录项
     */
    @Operation(summary = "删除目录项")
    @DeleteMapping("/item/{itemId}")
    @RequirePermission("doc:delete")
    @OperationLog(module = "卷宗管理", action = "删除目录项")
    public Result<Void> deleteDossierItem(
            @PathVariable Long matterId,
            @PathVariable Long itemId) {
        dossierService.deleteDossierItem(itemId);
        return Result.success();
    }

    /**
     * 调整目录项排序
     */
    @Operation(summary = "调整目录项排序")
    @PutMapping("/reorder")
    @RequirePermission("doc:edit")
    @OperationLog(module = "卷宗管理", action = "调整目录排序")
    public Result<Void> reorderDossierItems(
            @PathVariable Long matterId,
            @RequestBody List<Long> itemIds) {
        dossierService.reorderDossierItems(matterId, itemIds);
        return Result.success();
    }
}

