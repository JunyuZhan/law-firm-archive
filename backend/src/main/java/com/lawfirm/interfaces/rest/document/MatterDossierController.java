package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.service.DossierAutoArchiveService;
import com.lawfirm.application.document.service.MatterDossierService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.domain.document.entity.MatterDossierItem;
import com.lawfirm.domain.document.repository.DocumentTemplateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 项目卷宗管理接口 */
@Tag(name = "项目卷宗管理")
@RestController
@RequestMapping("/matter/{matterId}/dossier")
@RequiredArgsConstructor
public class MatterDossierController {

  /** 项目卷宗服务 */
  private final MatterDossierService dossierService;

  /** 卷宗自动归档服务 */
  private final DossierAutoArchiveService autoArchiveService;

  /** 文档模板仓储 */
  private final DocumentTemplateRepository documentTemplateRepository;

  /**
   * 获取项目卷宗目录
   *
   * @param matterId 案件ID
   * @return 卷宗目录项列表
   */
  @Operation(summary = "获取项目卷宗目录")
  @GetMapping
  @RequirePermission("doc:list")
  public Result<List<MatterDossierItem>> getDossierItems(@PathVariable final Long matterId) {
    return Result.success(dossierService.getDossierItems(matterId));
  }

  /**
   * 初始化项目卷宗目录
   *
   * @param matterId 案件ID
   * @return 卷宗目录项列表
   */
  @Operation(summary = "初始化项目卷宗目录")
  @PostMapping("/init")
  @RequirePermission("doc:create")
  @OperationLog(module = "卷宗管理", action = "初始化卷宗目录")
  public Result<List<MatterDossierItem>> initializeDossier(@PathVariable final Long matterId) {
    dossierService.initializeDossier(matterId);
    return Result.success(dossierService.getDossierItems(matterId));
  }

  /**
   * 添加自定义目录项
   *
   * @param matterId 案件ID
   * @param params 参数（包含parentId、name、itemType）
   * @return 目录项信息
   */
  @Operation(summary = "添加自定义目录项")
  @PostMapping("/item")
  @RequirePermission("doc:create")
  @OperationLog(module = "卷宗管理", action = "添加目录项")
  public Result<MatterDossierItem> addDossierItem(
      @PathVariable final Long matterId, @RequestBody final Map<String, Object> params) {
    Long parentId =
        params.get("parentId") != null ? Long.valueOf(params.get("parentId").toString()) : 0L;
    String name = (String) params.get("name");
    String itemType = (String) params.get("itemType");

    return Result.success(dossierService.addDossierItem(matterId, parentId, name, itemType));
  }

  /**
   * 更新目录项
   *
   * @param matterId 案件ID
   * @param itemId 目录项ID
   * @param params 参数（包含name、sortOrder）
   * @return 目录项信息
   */
  @Operation(summary = "更新目录项")
  @PutMapping("/item/{itemId}")
  @RequirePermission("doc:update")
  @OperationLog(module = "卷宗管理", action = "更新目录项")
  public Result<MatterDossierItem> updateDossierItem(
      @PathVariable final Long matterId,
      @PathVariable final Long itemId,
      @RequestBody final Map<String, Object> params) {
    String name = (String) params.get("name");
    Integer sortOrder =
        params.get("sortOrder") != null
            ? Integer.valueOf(params.get("sortOrder").toString())
            : null;

    return Result.success(dossierService.updateDossierItem(itemId, name, sortOrder));
  }

  /**
   * 删除目录项
   *
   * @param matterId 案件ID
   * @param itemId 目录项ID
   * @return 空结果
   */
  @Operation(summary = "删除目录项")
  @DeleteMapping("/item/{itemId}")
  @RequirePermission("doc:delete")
  @OperationLog(module = "卷宗管理", action = "删除目录项")
  public Result<Void> deleteDossierItem(
      @PathVariable final Long matterId, @PathVariable final Long itemId) {
    dossierService.deleteDossierItem(itemId);
    return Result.success();
  }

  /**
   * 调整目录项排序
   *
   * @param matterId 案件ID
   * @param itemIds 目录项ID列表（按顺序）
   * @return 空结果
   */
  @Operation(summary = "调整目录项排序")
  @PutMapping("/reorder")
  @RequirePermission("doc:update")
  @OperationLog(module = "卷宗管理", action = "调整目录排序")
  public Result<Void> reorderDossierItems(
      @PathVariable final Long matterId, @RequestBody final List<Long> itemIds) {
    dossierService.reorderDossierItems(matterId, itemIds);
    return Result.success();
  }

  // ==================== 自动归档管理 ====================

  /**
   * 重新生成授权委托书
   *
   * <p>使用场景： - 模板更新后需要重新生成 - 项目信息变更后需要更新 - 之前无模板时用了默认格式，模板准备好后重新生成
   *
   * @param matterId 案件ID
   * @return 生成结果
   */
  @Operation(summary = "重新生成授权委托书", description = "使用最新模板重新生成授权委托书，会覆盖已有的版本")
  @PostMapping("/regenerate/power-of-attorney")
  @RequirePermission("doc:create")
  @OperationLog(module = "卷宗管理", action = "重新生成授权委托书")
  public Result<Map<String, Object>> regeneratePowerOfAttorney(@PathVariable final Long matterId) {
    Map<String, Object> result = new HashMap<>();

    // 检查是否有可用模板
    DocumentTemplate template =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_POWER_OF_ATTORNEY);

    // 执行重新生成
    autoArchiveService.regeneratePowerOfAttorney(matterId);

    if (template != null) {
      result.put("success", true);
      result.put("message", "授权委托书重新生成成功");
      result.put("templateUsed", true);
      result.put("templateName", template.getName());
    } else {
      result.put("success", true);
      result.put("message", "授权委托书重新生成成功（使用默认格式）");
      result.put("templateUsed", false);
      result.put("hint", "建议配置授权委托书模板后再次重新生成");
    }

    return Result.success(result);
  }

  /**
   * 触发自动归档 用于手动触发归档（如果之前归档失败或需要补充归档）
   *
   * @param matterId 案件ID
   * @param contractId 合同ID（可选）
   * @return 执行结果
   */
  @Operation(summary = "触发自动归档", description = "手动触发项目卷宗自动归档")
  @PostMapping("/auto-archive")
  @RequirePermission("doc:create")
  @OperationLog(module = "卷宗管理", action = "触发自动归档")
  public Result<Map<String, String>> triggerAutoArchive(
      @PathVariable final Long matterId, @RequestParam(required = false) final Long contractId) {
    autoArchiveService.archiveMatterDocuments(matterId, contractId);

    Map<String, String> result = new HashMap<>();
    result.put("message", "自动归档已触发，请刷新查看卷宗目录");
    return Result.success(result);
  }
}
