package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.service.MatterDossierService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.document.entity.DossierTemplate;
import com.lawfirm.domain.document.entity.DossierTemplateItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 卷宗模板管理接口
 */
@Tag(name = "卷宗模板管理")
@RestController
@RequestMapping("/dossier/template")
@RequiredArgsConstructor
public class DossierTemplateController {

    private final MatterDossierService dossierService;

    /**
     * 获取所有卷宗模板
     */
    @Operation(summary = "获取所有卷宗模板")
    @GetMapping
    @RequirePermission("doc:list")
    public Result<List<DossierTemplate>> getAllTemplates() {
        return Result.success(dossierService.getAllTemplates());
    }

    /**
     * 获取模板目录项
     */
    @Operation(summary = "获取模板目录项")
    @GetMapping("/{templateId}/items")
    @RequirePermission("doc:list")
    public Result<List<DossierTemplateItem>> getTemplateItems(@PathVariable Long templateId) {
        return Result.success(dossierService.getTemplateItems(templateId));
    }

    /**
     * 创建卷宗模板
     */
    @Operation(summary = "创建卷宗模板")
    @PostMapping
    @RequirePermission("doc:list")
    public Result<DossierTemplate> createTemplate(@RequestBody DossierTemplate template) {
        return Result.success(dossierService.createTemplate(template));
    }

    /**
     * 更新卷宗模板
     */
    @Operation(summary = "更新卷宗模板")
    @PutMapping("/{id}")
    @RequirePermission("doc:list")
    public Result<DossierTemplate> updateTemplate(@PathVariable Long id, @RequestBody DossierTemplate template) {
        template.setId(id);
        return Result.success(dossierService.updateTemplate(template));
    }

    /**
     * 删除卷宗模板
     */
    @Operation(summary = "删除卷宗模板")
    @DeleteMapping("/{id}")
    @RequirePermission("doc:list")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        dossierService.deleteTemplate(id);
        return Result.success();
    }

    /**
     * 添加模板目录项
     */
    @Operation(summary = "添加模板目录项")
    @PostMapping("/{templateId}/items")
    @RequirePermission("doc:list")
    public Result<DossierTemplateItem> addTemplateItem(@PathVariable Long templateId,
            @RequestBody DossierTemplateItem item) {
        item.setTemplateId(templateId);
        return Result.success(dossierService.addTemplateItem(item));
    }

    /**
     * 更新模板目录项
     */
    @Operation(summary = "更新模板目录项")
    @PutMapping("/items/{itemId}")
    @RequirePermission("doc:list")
    public Result<DossierTemplateItem> updateTemplateItem(@PathVariable Long itemId,
            @RequestBody DossierTemplateItem item) {
        item.setId(itemId);
        return Result.success(dossierService.updateTemplateItem(item));
    }

    /**
     * 删除模板目录项
     */
    @Operation(summary = "删除模板目录项")
    @DeleteMapping("/items/{itemId}")
    @RequirePermission("doc:list")
    public Result<Void> deleteTemplateItem(@PathVariable Long itemId) {
        dossierService.deleteTemplateItem(itemId);
        return Result.success();
    }
}
