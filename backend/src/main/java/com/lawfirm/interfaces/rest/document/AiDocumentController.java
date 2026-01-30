package com.lawfirm.interfaces.rest.document;

import com.lawfirm.application.document.command.AiGenerateDocumentCommand;
import com.lawfirm.application.document.dto.MaskingMappingDTO;
import com.lawfirm.application.document.dto.MatterContextDTO;
import com.lawfirm.application.document.service.AiDocumentService;
import com.lawfirm.application.document.service.DataMaskingService;
import com.lawfirm.application.document.service.MatterContextCollector;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AI 文书生成控制器 */
@Tag(name = "AI文书生成", description = "使用 AI 大模型生成法律文书")
@RestController
@RequestMapping("/document/ai")
@RequiredArgsConstructor
public class AiDocumentController {

  /** AI文档服务 */
  private final AiDocumentService aiDocumentService;

  /** 项目上下文收集器 */
  private final MatterContextCollector matterContextCollector;

  /** 数据脱敏服务 */
  private final DataMaskingService dataMaskingService;

  /**
   * 检查 AI 服务状态
   *
   * @return AI服务状态信息
   */
  @Operation(summary = "检查 AI 服务状态", description = "检查 AI 大模型服务是否可用")
  @GetMapping("/status")
  public Result<Map<String, Object>> checkStatus() {
    return Result.success(aiDocumentService.checkAiStatus());
  }

  /**
   * AI 生成文书
   *
   * @param command 生成文书命令
   * @return 生成的文书信息
   */
  @Operation(summary = "AI 生成文书", description = "使用 AI 大模型生成法律文书")
  @PostMapping("/generate")
  public Result<Map<String, Object>> generateDocument(
      @Valid @RequestBody final AiGenerateDocumentCommand command) {
    return Result.success(aiDocumentService.generateDocument(command));
  }

  /**
   * AI 预览文书
   *
   * @param command 生成文书命令
   * @return 预览的文书内容
   */
  @Operation(summary = "AI 预览文书", description = "预览 AI 生成的文书内容（不保存）")
  @PostMapping("/preview")
  public Result<String> previewDocument(
      @Valid @RequestBody final AiGenerateDocumentCommand command) {
    return Result.success(aiDocumentService.previewDocument(command));
  }

  // ========== 项目上下文收集 ==========

  /**
   * 收集项目上下文信息
   *
   * @param matterId 案件ID
   * @param includeDocuments 是否包含文档信息
   * @param extractContent 是否提取文档内容
   * @return 项目上下文信息
   */
  @Operation(summary = "收集项目上下文信息", description = "一键收集项目的完整信息，用于 AI 文书生成")
  @GetMapping("/context/{matterId}")
  public Result<MatterContextDTO> collectMatterContext(
      @PathVariable final Long matterId,
      @RequestParam(defaultValue = "true") final boolean includeDocuments,
      @RequestParam(defaultValue = "false") final boolean extractContent) {
    MatterContextCollector.CollectOptions options = new MatterContextCollector.CollectOptions();
    options.setIncludeDocuments(includeDocuments);
    options.setExtractDocumentContent(extractContent);
    MatterContextDTO context = matterContextCollector.collectContext(matterId, options);
    return Result.success(context);
  }

  /**
   * 选择性收集项目上下文
   *
   * @param matterId 案件ID
   * @param request 收集选项请求
   * @return 项目上下文信息
   */
  @Operation(summary = "选择性收集项目上下文", description = "自定义选择要收集的信息类型和文档")
  @PostMapping("/context/{matterId}/selective")
  public Result<MatterContextDTO> collectSelectiveContext(
      @PathVariable final Long matterId, @RequestBody final CollectOptionsRequest request) {
    MatterContextCollector.CollectOptions options = new MatterContextCollector.CollectOptions();
    options.setIncludeMatterInfo(request.isIncludeMatterInfo());
    options.setIncludeClients(request.isIncludeClients());
    options.setIncludeParticipants(request.isIncludeParticipants());
    options.setIncludeDocuments(request.isIncludeDocuments());
    options.setExtractDocumentContent(request.isExtractDocumentContent());
    options.setSelectedDocumentIds(request.getSelectedDocumentIds());

    MatterContextDTO context = matterContextCollector.collectContext(matterId, options);
    return Result.success(context);
  }

  /**
   * 获取项目可选文档列表
   *
   * @param matterId 案件ID
   * @return 可选文档列表
   */
  @Operation(summary = "获取项目可选文档列表", description = "获取项目下所有可选的文档，供前端展示勾选")
  @GetMapping("/context/{matterId}/documents")
  public Result<List<MatterContextDTO.DocumentInfo>> getAvailableDocuments(
      @PathVariable final Long matterId) {
    List<MatterContextDTO.DocumentInfo> documents =
        matterContextCollector.getAvailableDocuments(matterId);
    return Result.success(documents);
  }

  // ========== 脱敏处理 ==========

  /**
   * 脱敏项目上下文信息
   *
   * @param context 项目上下文信息
   * @return 脱敏后的项目上下文信息
   */
  @Operation(summary = "脱敏项目上下文信息", description = "对收集的项目信息进行脱敏处理")
  @PostMapping("/context/mask")
  public Result<MatterContextDTO> maskMatterContext(@RequestBody final MatterContextDTO context) {
    MatterContextDTO maskedContext = dataMaskingService.maskContext(context);
    return Result.success(maskedContext);
  }

  /**
   * 脱敏并返回映射关系
   *
   * @param context 项目上下文信息
   * @return 脱敏结果响应（包含脱敏后的上下文和映射关系）
   */
  @Operation(summary = "脱敏并返回映射关系", description = "脱敏处理并返回原始值与脱敏值的映射，用于后续还原")
  @PostMapping("/context/mask-with-mapping")
  public Result<MaskingResultResponse> maskWithMapping(
      @RequestBody final MatterContextDTO context) {
    DataMaskingService.MaskingResult result = dataMaskingService.maskContextWithMapping(context);

    MaskingResultResponse response = new MaskingResultResponse();
    response.setMaskedContext(result.getMaskedContext());
    response.setMapping(result.getMapping());

    return Result.success(response);
  }

  /**
   * 收集并脱敏项目上下文
   *
   * @param matterId 案件ID
   * @param includeDocuments 是否包含文档信息
   * @param extractContent 是否提取文档内容
   * @return 脱敏后的项目上下文信息
   */
  @Operation(summary = "收集并脱敏项目上下文", description = "一键收集项目信息并脱敏")
  @GetMapping("/context/{matterId}/masked")
  public Result<MatterContextDTO> collectAndMaskContext(
      @PathVariable final Long matterId,
      @RequestParam(defaultValue = "true") final boolean includeDocuments,
      @RequestParam(defaultValue = "false") final boolean extractContent) {
    MatterContextCollector.CollectOptions options = new MatterContextCollector.CollectOptions();
    options.setIncludeDocuments(includeDocuments);
    options.setExtractDocumentContent(extractContent);

    MatterContextDTO context = matterContextCollector.collectContext(matterId, options);
    MatterContextDTO maskedContext = dataMaskingService.maskContext(context);
    return Result.success(maskedContext);
  }

  /**
   * 收集并脱敏（带映射）
   *
   * @param matterId 案件ID
   * @param request 收集选项请求
   * @return 脱敏结果响应（包含脱敏后的上下文和映射关系）
   */
  @Operation(summary = "收集并脱敏（带映射）", description = "一键收集、脱敏并返回映射关系")
  @PostMapping("/context/{matterId}/masked-with-mapping")
  public Result<MaskingResultResponse> collectAndMaskWithMapping(
      @PathVariable final Long matterId,
      @RequestBody(required = false) final CollectOptionsRequest request) {
    MatterContextCollector.CollectOptions options = new MatterContextCollector.CollectOptions();
    if (request != null) {
      options.setIncludeMatterInfo(request.isIncludeMatterInfo());
      options.setIncludeClients(request.isIncludeClients());
      options.setIncludeParticipants(request.isIncludeParticipants());
      options.setIncludeDocuments(request.isIncludeDocuments());
      options.setExtractDocumentContent(request.isExtractDocumentContent());
      options.setSelectedDocumentIds(request.getSelectedDocumentIds());
    }

    MatterContextDTO context = matterContextCollector.collectContext(matterId, options);
    DataMaskingService.MaskingResult result = dataMaskingService.maskContextWithMapping(context);

    MaskingResultResponse response = new MaskingResultResponse();
    response.setMaskedContext(result.getMaskedContext());
    response.setMapping(result.getMapping());

    return Result.success(response);
  }

  // ========== 脱敏还原 ==========

  /**
   * 脱敏还原
   *
   * @param request 还原请求
   * @return 还原后的文书内容
   */
  @Operation(summary = "脱敏还原", description = "将脱敏后的文书内容还原为包含真实信息的文书")
  @PostMapping("/restore")
  public Result<String> restoreMaskedText(@RequestBody final RestoreRequest request) {
    String restoredText =
        dataMaskingService.restoreMaskedText(request.getMaskedText(), request.getMapping());
    return Result.success(restoredText);
  }

  // ========== 请求/响应 DTO ==========

  /** 收集选项请求 */
  @Data
  public static class CollectOptionsRequest {
    /** 是否包含项目信息 */
    private boolean includeMatterInfo = true;

    /** 是否包含客户信息 */
    private boolean includeClients = true;

    /** 是否包含参与人信息 */
    private boolean includeParticipants = true;

    /** 是否包含文档信息 */
    private boolean includeDocuments = false;

    /** 是否提取文档内容 */
    private boolean extractDocumentContent = false;

    /** 选中的文档ID集合 */
    private Set<Long> selectedDocumentIds;
  }

  /** 脱敏结果响应 */
  @Data
  public static class MaskingResultResponse {
    /** 脱敏后的项目上下文 */
    private MatterContextDTO maskedContext;

    /** 脱敏映射关系 */
    private MaskingMappingDTO mapping;
  }

  /** 脱敏还原请求 */
  @Data
  public static class RestoreRequest {
    /** AI 生成的脱敏文书内容 */
    private String maskedText;

    /** 脱敏映射关系 */
    private MaskingMappingDTO mapping;
  }
}
