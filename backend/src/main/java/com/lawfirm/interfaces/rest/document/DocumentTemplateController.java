package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.BatchGenerateDocumentCommand;
import com.lawfirm.application.document.command.CreateDocumentTemplateCommand;
import com.lawfirm.application.document.command.GenerateDocumentCommand;
import com.lawfirm.application.document.command.PreviewTemplateCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.DocumentTemplateDTO;
import com.lawfirm.application.document.dto.DocumentTemplateQueryDTO;
import com.lawfirm.application.document.service.DocumentTemplateAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文档模板接口
 */
@Tag(name = "文档模板", description = "文档模板管理相关接口")
@RestController
@RequestMapping("/api/document-templates")
@RequiredArgsConstructor
public class DocumentTemplateController {

    private final DocumentTemplateAppService templateAppService;

    /**
     * 分页查询模板
     */
    @GetMapping
    @RequirePermission("doc:template:list")
    public Result<PageResult<DocumentTemplateDTO>> list(DocumentTemplateQueryDTO query) {
        return Result.success(templateAppService.listTemplates(query));
    }

    /**
     * 获取模板详情
     */
    @GetMapping("/{id}")
    @RequirePermission("doc:template:detail")
    public Result<DocumentTemplateDTO> getById(@PathVariable Long id) {
        return Result.success(templateAppService.getTemplateById(id));
    }

    /**
     * 创建模板
     */
    @PostMapping
    @RequirePermission("doc:template:manage")
    @OperationLog(module = "文档模板", action = "创建模板")
    public Result<DocumentTemplateDTO> create(@Valid @RequestBody CreateDocumentTemplateCommand command) {
        return Result.success(templateAppService.createTemplate(command));
    }

    /**
     * 更新模板
     */
    @PutMapping("/{id}")
    @RequirePermission("doc:template:manage")
    @OperationLog(module = "文档模板", action = "更新模板")
    public Result<DocumentTemplateDTO> update(@PathVariable Long id,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String templateType,
                                              @RequestParam(required = false) String description,
                                              @RequestParam(required = false) String status) {
        return Result.success(templateAppService.updateTemplate(id, name, categoryId, templateType, description, status));
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/{id}")
    @RequirePermission("doc:template:manage")
    @OperationLog(module = "文档模板", action = "删除模板")
    public Result<Void> delete(@PathVariable Long id) {
        templateAppService.deleteTemplate(id);
        return Result.success();
    }

    /**
     * 使用模板
     */
    @PostMapping("/{id}/use")
    @RequirePermission("doc:template:use")
    @OperationLog(module = "文档模板", action = "使用模板")
    public Result<Void> use(@PathVariable Long id) {
        templateAppService.useTemplate(id);
        return Result.success();
    }

    /**
     * 从模板生成文档（M5-033）
     */
    @PostMapping("/generate")
    @RequirePermission("doc:template:generate")
    @Operation(summary = "从模板生成文档", description = "根据模板和案件信息生成文档")
    @OperationLog(module = "文档模板", action = "生成文档")
    public Result<DocumentDTO> generateDocument(@RequestBody @Valid GenerateDocumentCommand command) {
        return Result.success(templateAppService.generateDocument(command));
    }

    /**
     * 批量生成文档（M5-034）
     */
    @PostMapping("/batch-generate")
    @RequirePermission("doc:template:generate")
    @Operation(summary = "批量生成文档", description = "根据模板批量生成多份文档")
    @OperationLog(module = "文档模板", action = "批量生成文档")
    public Result<List<DocumentDTO>> batchGenerateDocuments(@RequestBody @Valid BatchGenerateDocumentCommand command) {
        return Result.success(templateAppService.batchGenerateDocuments(command));
    }

    /**
     * 预览模板（M5-035）
     */
    @PostMapping("/preview")
    @RequirePermission("doc:template:view")
    @Operation(summary = "预览模板", description = "预览模板效果，查看变量替换后的内容")
    public Result<Map<String, Object>> previewTemplate(@RequestBody @Valid PreviewTemplateCommand command) {
        return Result.success(templateAppService.previewTemplate(command));
    }
}
