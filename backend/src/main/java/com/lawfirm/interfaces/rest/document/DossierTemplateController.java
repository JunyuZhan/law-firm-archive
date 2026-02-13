package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.service.MatterDossierService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.document.entity.DossierTemplate;
import com.lawfirm.domain.document.entity.DossierTemplateItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 卷宗模板管理接口 */
@Tag(name = "卷宗模板管理")
@RestController
@RequestMapping("/dossier/template")
@RequiredArgsConstructor
public class DossierTemplateController {

  /** 项目卷宗服务 */
  private final MatterDossierService dossierService;

  /**
   * 获取所有卷宗模板
   *
   * @return 卷宗模板列表
   */
  @Operation(summary = "获取所有卷宗模板")
  @GetMapping
  @RequirePermission("doc:list")
  public Result<List<DossierTemplate>> getAllTemplates() {
    return Result.success(dossierService.getAllTemplates());
  }

  /**
   * 获取模板目录项
   *
   * @param templateId 模板ID
   * @return 目录项列表
   */
  @Operation(summary = "获取模板目录项")
  @GetMapping("/{templateId}/items")
  @RequirePermission("doc:list")
  public Result<List<DossierTemplateItem>> getTemplateItems(@PathVariable final Long templateId) {
    return Result.success(dossierService.getTemplateItems(templateId));
  }

  /**
   * 创建卷宗模板
   *
   * @param template 卷宗模板
   * @return 卷宗模板信息
   */
  @Operation(summary = "创建卷宗模板")
  @PostMapping
  @RequirePermission("doc:list")
  public Result<DossierTemplate> createTemplate(@RequestBody final DossierTemplate template) {
    return Result.success(dossierService.createTemplate(template));
  }

  /**
   * 更新卷宗模板
   *
   * @param id 模板ID
   * @param template 卷宗模板
   * @return 卷宗模板信息
   */
  @Operation(summary = "更新卷宗模板")
  @PutMapping("/{id}")
  @RequirePermission("doc:list")
  public Result<DossierTemplate> updateTemplate(
      @PathVariable final Long id, @RequestBody final DossierTemplate template) {
    template.setId(id);
    return Result.success(dossierService.updateTemplate(template));
  }

  /**
   * 删除卷宗模板
   *
   * @param id 模板ID
   * @return 空结果
   */
  @Operation(summary = "删除卷宗模板")
  @DeleteMapping("/{id}")
  @RequirePermission("doc:list")
  public Result<Void> deleteTemplate(@PathVariable final Long id) {
    dossierService.deleteTemplate(id);
    return Result.success();
  }

  /**
   * 添加模板目录项
   *
   * @param templateId 模板ID
   * @param item 目录项
   * @return 目录项信息
   */
  @Operation(summary = "添加模板目录项")
  @PostMapping("/{templateId}/items")
  @RequirePermission("doc:list")
  public Result<DossierTemplateItem> addTemplateItem(
      @PathVariable final Long templateId, @RequestBody final DossierTemplateItem item) {
    item.setTemplateId(templateId);
    return Result.success(dossierService.addTemplateItem(item));
  }

  /**
   * 更新模板目录项
   *
   * @param itemId 目录项ID
   * @param item 目录项
   * @return 目录项信息
   */
  @Operation(summary = "更新模板目录项")
  @PutMapping("/items/{itemId}")
  @RequirePermission("doc:list")
  public Result<DossierTemplateItem> updateTemplateItem(
      @PathVariable final Long itemId, @RequestBody final DossierTemplateItem item) {
    item.setId(itemId);
    return Result.success(dossierService.updateTemplateItem(item));
  }

  /**
   * 删除模板目录项
   *
   * @param itemId 目录项ID
   * @return 空结果
   */
  @Operation(summary = "删除模板目录项")
  @DeleteMapping("/items/{itemId}")
  @RequirePermission("doc:list")
  public Result<Void> deleteTemplateItem(@PathVariable final Long itemId) {
    dossierService.deleteTemplateItem(itemId);
    return Result.success();
  }
}
