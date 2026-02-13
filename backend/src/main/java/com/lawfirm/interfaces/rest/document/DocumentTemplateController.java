package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.BatchGenerateDocumentCommand;
import com.lawfirm.application.document.command.CreateDocumentTemplateCommand;
import com.lawfirm.application.document.command.GenerateDocumentCommand;
import com.lawfirm.application.document.command.PreviewTemplateCommand;
import com.lawfirm.application.document.command.UpdateDocumentTemplateCommand;
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
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 文档模板接口 */
@Tag(name = "文档模板", description = "文档模板管理相关接口")
@RestController
@RequestMapping("/document/template")
@RequiredArgsConstructor
public class DocumentTemplateController {

  /** 文档模板应用服务 */
  private final DocumentTemplateAppService templateAppService;

  /**
   * 分页查询模板
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping
  @RequirePermission("doc:template:list")
  public Result<PageResult<DocumentTemplateDTO>> list(final DocumentTemplateQueryDTO query) {
    return Result.success(templateAppService.listTemplates(query));
  }

  /**
   * 获取启用的模板列表（公共接口） 供文书制作页面选择模板使用，无需特殊权限
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @GetMapping("/active")
  @Operation(summary = "获取启用的模板列表（公共）", description = "供文书制作页面选择模板使用")
  public Result<PageResult<DocumentTemplateDTO>> listActiveTemplates(
      final DocumentTemplateQueryDTO query) {
    // 强制只返回启用状态的模板
    query.setStatus("ENABLED");
    return Result.success(templateAppService.listTemplates(query));
  }

  /**
   * 获取模板详情
   *
   * @param id 模板ID
   * @return 模板详情
   */
  @GetMapping("/{id}")
  @RequirePermission("doc:template:detail")
  public Result<DocumentTemplateDTO> getById(@PathVariable final Long id) {
    return Result.success(templateAppService.getTemplateById(id));
  }

  /**
   * 创建模板
   *
   * @param command 创建模板命令
   * @return 模板信息
   */
  @PostMapping
  @RequirePermission("doc:template:manage")
  @OperationLog(module = "文档模板", action = "创建模板")
  public Result<DocumentTemplateDTO> create(
      @Valid @RequestBody final CreateDocumentTemplateCommand command) {
    return Result.success(templateAppService.createTemplate(command));
  }

  /**
   * 更新模板
   *
   * @param id 模板ID
   * @param command 更新模板命令
   * @return 模板信息
   */
  @PutMapping("/{id}")
  @RequirePermission("doc:template:manage")
  @OperationLog(module = "文档模板", action = "更新模板")
  public Result<DocumentTemplateDTO> update(
      @PathVariable final Long id,
      @Valid @RequestBody final UpdateDocumentTemplateCommand command) {
    return Result.success(templateAppService.updateTemplate(id, command));
  }

  /**
   * 删除模板
   *
   * @param id 模板ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("doc:template:manage")
  @OperationLog(module = "文档模板", action = "删除模板")
  public Result<Void> delete(@PathVariable final Long id) {
    templateAppService.deleteTemplate(id);
    return Result.success();
  }

  /**
   * 使用模板
   *
   * @param id 模板ID
   * @return 空结果
   */
  @PostMapping("/{id}/use")
  @RequirePermission("doc:template:use")
  @OperationLog(module = "文档模板", action = "使用模板")
  public Result<Void> use(@PathVariable final Long id) {
    templateAppService.useTemplate(id);
    return Result.success();
  }

  /**
   * 从模板生成文档（M5-033）
   *
   * @param command 生成文档命令
   * @return 生成的文档信息
   */
  @PostMapping("/generate")
  @RequirePermission("doc:template:generate")
  @Operation(summary = "从模板生成文档", description = "根据模板和案件信息生成文档")
  @OperationLog(module = "文档模板", action = "生成文档")
  public Result<DocumentDTO> generateDocument(
      @RequestBody @Valid final GenerateDocumentCommand command) {
    return Result.success(templateAppService.generateDocument(command));
  }

  /**
   * 批量生成文档（M5-034）
   *
   * @param command 批量生成文档命令
   * @return 生成的文档列表
   */
  @PostMapping("/batch-generate")
  @RequirePermission("doc:template:generate")
  @Operation(summary = "批量生成文档", description = "根据模板批量生成多份文档")
  @OperationLog(module = "文档模板", action = "批量生成文档")
  public Result<List<DocumentDTO>> batchGenerateDocuments(
      @RequestBody @Valid final BatchGenerateDocumentCommand command) {
    return Result.success(templateAppService.batchGenerateDocuments(command));
  }

  /**
   * 预览模板（M5-035）
   *
   * @param command 预览模板命令
   * @return 预览结果
   */
  @PostMapping("/preview")
  @RequirePermission("doc:template:view")
  @Operation(summary = "预览模板", description = "预览模板效果，查看变量替换后的内容")
  public Result<Map<String, Object>> previewTemplate(
      @RequestBody @Valid final PreviewTemplateCommand command) {
    return Result.success(templateAppService.previewTemplate(command));
  }
}
