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
}

