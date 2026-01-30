package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.service.DossierAutoArchiveService;
import com.lawfirm.application.document.service.TemplateVariableService;
import com.lawfirm.common.result.Result;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.domain.document.repository.DocumentTemplateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 卷宗自动归档测试控制器（仅用于开发测试） */
@RestController
@RequestMapping("/test/dossier-archive")
@Tag(name = "测试接口", description = "开发测试接口")
@RequiredArgsConstructor
public class DossierAutoArchiveTestController {

  /** 卷宗自动归档服务 */
  private final DossierAutoArchiveService dossierAutoArchiveService;

  /** 模板变量服务 */
  private final TemplateVariableService templateVariableService;

  /** 文档模板仓储 */
  private final DocumentTemplateRepository documentTemplateRepository;

  /**
   * 手动触发自动归档（同步执行）
   *
   * @param matterId 案件ID
   * @param contractId 合同ID（可选）
   * @return 执行结果
   */
  @Operation(summary = "手动触发自动归档测试")
  @PostMapping("/trigger/{matterId}")
  public Result<String> triggerArchive(
      @PathVariable final Long matterId, @RequestParam(required = false) final Long contractId) {
    try {
      // 同步调用以便看到结果
      dossierAutoArchiveService.archiveMatterDocuments(matterId, contractId);
      return Result.success("自动归档触发成功，请检查卷宗目录");
    } catch (Exception e) {
      return Result.fail("自动归档失败: " + e.getMessage());
    }
  }

  /**
   * 手动触发自动归档（异步执行）
   *
   * @param matterId 案件ID
   * @param contractId 合同ID（可选）
   * @return 执行结果
   */
  @Operation(summary = "手动触发自动归档测试（异步）")
  @PostMapping("/trigger-async/{matterId}")
  public Result<String> triggerArchiveAsync(
      @PathVariable final Long matterId, @RequestParam(required = false) final Long contractId) {
    try {
      Long currentUserId = SecurityUtils.getUserIdOrDefault(1L);
      dossierAutoArchiveService.archiveMatterDocumentsAsync(matterId, contractId, currentUserId);
      return Result.success("自动归档已异步触发，请稍后检查卷宗目录");
    } catch (Exception e) {
      return Result.fail("自动归档触发失败: " + e.getMessage());
    }
  }

  /**
   * 单独归档收案审批表
   *
   * @param matterId 案件ID
   * @param contractId 合同ID
   * @return 执行结果
   */
  @PostMapping("/approval-form/{matterId}")
  public Result<String> archiveApprovalForm(
      @PathVariable final Long matterId, @RequestParam final Long contractId) {
    try {
      dossierAutoArchiveService.archiveApprovalForm(matterId, contractId);
      return Result.success("收案审批表归档成功");
    } catch (Exception e) {
      return Result.fail("收案审批表归档失败: " + e.getMessage());
    }
  }

  /**
   * 单独归档委托合同
   *
   * @param matterId 案件ID
   * @param contractId 合同ID
   * @return 执行结果
   */
  @PostMapping("/contract/{matterId}")
  public Result<String> archiveContract(
      @PathVariable final Long matterId, @RequestParam final Long contractId) {
    try {
      dossierAutoArchiveService.archiveContract(matterId, contractId);
      return Result.success("委托合同归档成功");
    } catch (Exception e) {
      return Result.fail("委托合同归档失败: " + e.getMessage());
    }
  }

  /**
   * 单独归档授权委托书
   *
   * @param matterId 案件ID
   * @return 执行结果
   */
  @PostMapping("/power-of-attorney/{matterId}")
  public Result<String> archivePowerOfAttorney(@PathVariable final Long matterId) {
    try {
      dossierAutoArchiveService.archivePowerOfAttorney(matterId);
      return Result.success("授权委托书归档成功");
    } catch (Exception e) {
      return Result.fail("授权委托书归档失败: " + e.getMessage());
    }
  }

  /**
   * 重新生成授权委托书（强制覆盖已有的）
   *
   * <p>使用场景： - 模板更新后需要重新生成 - 项目信息变更后需要更新 - 之前无模板时用了默认格式，模板准备好后重新生成
   *
   * @param matterId 案件ID
   * @return 执行结果
   */
  @Operation(summary = "重新生成授权委托书（强制覆盖）")
  @PostMapping("/power-of-attorney/{matterId}/regenerate")
  public Result<String> regeneratePowerOfAttorney(@PathVariable final Long matterId) {
    try {
      // 检查是否有可用模板
      DocumentTemplate template =
          documentTemplateRepository.findFirstByTemplateType(
              DossierAutoArchiveService.TEMPLATE_TYPE_POWER_OF_ATTORNEY);

      dossierAutoArchiveService.regeneratePowerOfAttorney(matterId);

      String message;
      if (template != null) {
        message = String.format("授权委托书重新生成成功（使用模板：%s）", template.getName());
      } else {
        message = "授权委托书重新生成成功（使用默认格式，建议配置模板后再次重新生成）";
      }

      return Result.success(message);
    } catch (Exception e) {
      return Result.fail("授权委托书重新生成失败: " + e.getMessage());
    }
  }

  // ==================== 模板预览功能 ====================

  /**
   * 预览收案审批表 用户可以在生成前查看内容
   *
   * @param matterId 案件ID
   * @param contractId 合同ID（可选）
   * @return 预览内容
   */
  @Operation(summary = "预览收案审批表（生成前查看）")
  @GetMapping("/preview/approval-form/{matterId}")
  public Result<Map<String, Object>> previewApprovalForm(
      @PathVariable final Long matterId, @RequestParam(required = false) final Long contractId) {

    Map<String, Object> result = new HashMap<>();

    // 获取模板
    DocumentTemplate template =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_APPROVAL_FORM);

    if (template == null) {
      result.put("templateExists", false);
      result.put("message", "未找到收案审批表模板，将使用默认格式生成");
      result.put("content", null);
    } else {
      result.put("templateExists", true);
      result.put("templateNo", template.getTemplateNo());
      result.put("templateName", template.getName());

      // 收集变量并替换
      Map<String, Object> variables =
          templateVariableService.collectVariablesWithContractAndApproval(
              matterId, contractId, null);
      String content = templateVariableService.replaceVariables(template.getContent(), variables);

      result.put("content", content);
      result.put("variables", variables);
    }

    return Result.success(result);
  }

  /**
   * 预览授权委托书 用户可以在生成前查看内容
   *
   * @param matterId 案件ID
   * @return 预览内容
   */
  @Operation(summary = "预览授权委托书（生成前查看）")
  @GetMapping("/preview/power-of-attorney/{matterId}")
  public Result<Map<String, Object>> previewPowerOfAttorney(@PathVariable final Long matterId) {

    Map<String, Object> result = new HashMap<>();

    // 获取模板
    DocumentTemplate template =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_POWER_OF_ATTORNEY);

    if (template == null) {
      result.put("templateExists", false);
      result.put("message", "未找到授权委托书模板，将使用默认格式生成");
      result.put("content", null);
    } else {
      result.put("templateExists", true);
      result.put("templateNo", template.getTemplateNo());
      result.put("templateName", template.getName());

      // 收集变量并替换
      Map<String, Object> variables = templateVariableService.collectVariables(matterId);
      String content = templateVariableService.replaceVariables(template.getContent(), variables);

      result.put("content", content);
      result.put("variables", variables);
    }

    return Result.success(result);
  }

  /**
   * 预览委托合同 用户可以在生成前查看内容
   *
   * @param matterId 案件ID
   * @return 预览内容
   */
  @Operation(summary = "预览委托合同（生成前查看）")
  @GetMapping("/preview/contract/{matterId}")
  public Result<Map<String, Object>> previewContract(@PathVariable final Long matterId) {

    Map<String, Object> result = new HashMap<>();

    // 获取模板
    DocumentTemplate template =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_CONTRACT);

    if (template == null) {
      result.put("templateExists", false);
      result.put("message", "未找到委托合同模板，将使用默认格式生成");
      result.put("content", null);
    } else {
      result.put("templateExists", true);
      result.put("templateNo", template.getTemplateNo());
      result.put("templateName", template.getName());

      // 收集变量并替换
      Map<String, Object> variables = templateVariableService.collectVariables(matterId);
      String content = templateVariableService.replaceVariables(template.getContent(), variables);

      result.put("content", content);
      result.put("variables", variables);
    }

    return Result.success(result);
  }

  /**
   * 获取可用的归档模板列表
   *
   * @return 模板列表
   */
  @Operation(summary = "获取可用的归档模板列表")
  @GetMapping("/templates")
  public Result<Map<String, Object>> getAvailableTemplates() {
    Map<String, Object> result = new HashMap<>();

    // 收案审批表模板
    DocumentTemplate approvalTemplate =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_APPROVAL_FORM);
    result.put(
        "approvalForm",
        approvalTemplate != null
            ? Map.of(
                "templateNo", approvalTemplate.getTemplateNo(),
                "name", approvalTemplate.getName(),
                "description",
                    approvalTemplate.getDescription() != null
                        ? approvalTemplate.getDescription()
                        : "")
            : null);

    // 授权委托书模板
    DocumentTemplate poaTemplate =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_POWER_OF_ATTORNEY);
    result.put(
        "powerOfAttorney",
        poaTemplate != null
            ? Map.of(
                "templateNo", poaTemplate.getTemplateNo(),
                "name", poaTemplate.getName(),
                "description",
                    poaTemplate.getDescription() != null ? poaTemplate.getDescription() : "")
            : null);

    // 委托合同模板
    DocumentTemplate contractTemplate =
        documentTemplateRepository.findFirstByTemplateType(
            DossierAutoArchiveService.TEMPLATE_TYPE_CONTRACT);
    result.put(
        "contract",
        contractTemplate != null
            ? Map.of(
                "templateNo", contractTemplate.getTemplateNo(),
                "name", contractTemplate.getName(),
                "description",
                    contractTemplate.getDescription() != null
                        ? contractTemplate.getDescription()
                        : "")
            : null);

    return Result.success(result);
  }
}
