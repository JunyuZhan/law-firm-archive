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
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 文书生成控制器
 */
@Tag(name = "AI文书生成", description = "使用 AI 大模型生成法律文书")
@RestController
@RequestMapping("/document/ai")
@RequiredArgsConstructor
public class AiDocumentController {

    private final AiDocumentService aiDocumentService;
    private final MatterContextCollector matterContextCollector;
    private final DataMaskingService dataMaskingService;

    @Operation(summary = "检查 AI 服务状态", description = "检查 AI 大模型服务是否可用")
    @GetMapping("/status")
    public Result<Map<String, Object>> checkStatus() {
        return Result.success(aiDocumentService.checkAiStatus());
    }

    @Operation(summary = "AI 生成文书", description = "使用 AI 大模型生成法律文书")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateDocument(@Valid @RequestBody AiGenerateDocumentCommand command) {
        return Result.success(aiDocumentService.generateDocument(command));
    }

    @Operation(summary = "AI 预览文书", description = "预览 AI 生成的文书内容（不保存）")
    @PostMapping("/preview")
    public Result<String> previewDocument(@Valid @RequestBody AiGenerateDocumentCommand command) {
        return Result.success(aiDocumentService.previewDocument(command));
    }

    // ========== 项目上下文收集 ==========

    @Operation(summary = "收集项目上下文信息", description = "一键收集项目的完整信息，用于 AI 文书生成")
    @GetMapping("/context/{matterId}")
    public Result<MatterContextDTO> collectMatterContext(
            @PathVariable Long matterId,
            @RequestParam(defaultValue = "true") boolean includeDocuments,
            @RequestParam(defaultValue = "false") boolean extractContent) {
        MatterContextCollector.CollectOptions options = new MatterContextCollector.CollectOptions();
        options.setIncludeDocuments(includeDocuments);
        options.setExtractDocumentContent(extractContent);
        MatterContextDTO context = matterContextCollector.collectContext(matterId, options);
        return Result.success(context);
    }

    @Operation(summary = "选择性收集项目上下文", description = "自定义选择要收集的信息类型和文档")
    @PostMapping("/context/{matterId}/selective")
    public Result<MatterContextDTO> collectSelectiveContext(
            @PathVariable Long matterId,
            @RequestBody CollectOptionsRequest request) {
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

    @Operation(summary = "获取项目可选文档列表", description = "获取项目下所有可选的文档，供前端展示勾选")
    @GetMapping("/context/{matterId}/documents")
    public Result<List<MatterContextDTO.DocumentInfo>> getAvailableDocuments(@PathVariable Long matterId) {
        List<MatterContextDTO.DocumentInfo> documents = matterContextCollector.getAvailableDocuments(matterId);
        return Result.success(documents);
    }

    // ========== 脱敏处理 ==========

    @Operation(summary = "脱敏项目上下文信息", description = "对收集的项目信息进行脱敏处理")
    @PostMapping("/context/mask")
    public Result<MatterContextDTO> maskMatterContext(@RequestBody MatterContextDTO context) {
        MatterContextDTO maskedContext = dataMaskingService.maskContext(context);
        return Result.success(maskedContext);
    }

    @Operation(summary = "脱敏并返回映射关系", description = "脱敏处理并返回原始值与脱敏值的映射，用于后续还原")
    @PostMapping("/context/mask-with-mapping")
    public Result<MaskingResultResponse> maskWithMapping(@RequestBody MatterContextDTO context) {
        DataMaskingService.MaskingResult result = dataMaskingService.maskContextWithMapping(context);
        
        MaskingResultResponse response = new MaskingResultResponse();
        response.setMaskedContext(result.getMaskedContext());
        response.setMapping(result.getMapping());
        
        return Result.success(response);
    }

    @Operation(summary = "收集并脱敏项目上下文", description = "一键收集项目信息并脱敏")
    @GetMapping("/context/{matterId}/masked")
    public Result<MatterContextDTO> collectAndMaskContext(
            @PathVariable Long matterId,
            @RequestParam(defaultValue = "true") boolean includeDocuments,
            @RequestParam(defaultValue = "false") boolean extractContent) {
        MatterContextCollector.CollectOptions options = new MatterContextCollector.CollectOptions();
        options.setIncludeDocuments(includeDocuments);
        options.setExtractDocumentContent(extractContent);
        
        MatterContextDTO context = matterContextCollector.collectContext(matterId, options);
        MatterContextDTO maskedContext = dataMaskingService.maskContext(context);
        return Result.success(maskedContext);
    }

    @Operation(summary = "收集并脱敏（带映射）", description = "一键收集、脱敏并返回映射关系")
    @PostMapping("/context/{matterId}/masked-with-mapping")
    public Result<MaskingResultResponse> collectAndMaskWithMapping(
            @PathVariable Long matterId,
            @RequestBody(required = false) CollectOptionsRequest request) {
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

    @Operation(summary = "脱敏还原", description = "将脱敏后的文书内容还原为包含真实信息的文书")
    @PostMapping("/restore")
    public Result<String> restoreMaskedText(@RequestBody RestoreRequest request) {
        String restoredText = dataMaskingService.restoreMaskedText(request.getMaskedText(), request.getMapping());
        return Result.success(restoredText);
    }

    // ========== 请求/响应 DTO ==========

    /**
     * 收集选项请求
     */
    @Data
    public static class CollectOptionsRequest {
        private boolean includeMatterInfo = true;
        private boolean includeClients = true;
        private boolean includeParticipants = true;
        private boolean includeDocuments = false;
        private boolean extractDocumentContent = false;
        private Set<Long> selectedDocumentIds;
    }

    /**
     * 脱敏结果响应
     */
    @Data
    public static class MaskingResultResponse {
        private MatterContextDTO maskedContext;
        private MaskingMappingDTO mapping;
    }

    /**
     * 脱敏还原请求
     */
    @Data
    public static class RestoreRequest {
        /** AI 生成的脱敏文书内容 */
        private String maskedText;
        /** 脱敏映射关系 */
        private MaskingMappingDTO mapping;
    }
}
