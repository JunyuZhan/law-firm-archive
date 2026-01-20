package com.lawfirm.application.document.service;

import com.lawfirm.application.document.command.AiGenerateDocumentCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.MatterContextDTO;
import com.lawfirm.application.system.service.ExternalIntegrationAppService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.ExternalIntegration;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.ai.LlmClient;
import com.lawfirm.infrastructure.external.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AI 文书生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDocumentService {

    private final ExternalIntegrationAppService integrationAppService;
    private final LlmClient llmClient;
    private final MatterRepository matterRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentAppService documentAppService;
    private final MinioService minioService;

    /**
     * AI 生成文书
     */
    @Transactional
    public Map<String, Object> generateDocument(AiGenerateDocumentCommand command) {
        // 1. 获取 AI 集成配置（支持指定或使用默认）
        ExternalIntegration aiIntegration;
        if (command.getAiIntegrationId() != null) {
            // 使用用户指定的 AI 模型（获取解密后的配置）
            aiIntegration = integrationAppService.getIntegrationWithDecryptedKeys(
                    integrationAppService.getAIIntegrationById(command.getAiIntegrationId()).getId());
            if (!Boolean.TRUE.equals(aiIntegration.getEnabled())) {
                throw new BusinessException("指定的 AI 模型未启用");
            }
        } else {
            // 使用默认的启用 AI 模型（获取解密后的配置）
            ExternalIntegration enabledIntegration = integrationAppService.getEnabledAIIntegration();
            if (enabledIntegration == null) {
                throw new BusinessException("未配置或未启用 AI 大模型，请在系统管理-外部集成中配置");
            }
            aiIntegration = integrationAppService.getIntegrationWithDecryptedKeys(enabledIntegration.getId());
        }

        // 2. 构建 Prompt
        String systemPrompt = buildSystemPrompt(command);
        String userPrompt = buildUserPrompt(command);

        log.info("开始调用 AI 生成文书: type={}, model={}", 
                command.getDocumentType(), aiIntegration.getIntegrationCode());

        // 3. 调用大模型生成内容
        String generatedContent;
        try {
            generatedContent = llmClient.generate(aiIntegration, systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("AI 文书生成失败", e);
            throw new BusinessException("AI 文书生成失败: " + e.getMessage());
        }

        if (generatedContent == null || generatedContent.trim().isEmpty()) {
            throw new BusinessException("AI 返回内容为空，请重试");
        }

        log.info("AI 文书生成成功，内容长度: {}", generatedContent.length());

        // 4. 如果仅预览，直接返回
        if (Boolean.TRUE.equals(command.getPreviewOnly())) {
            Map<String, Object> result = new HashMap<>();
            result.put("content", generatedContent);
            result.put("preview", true);
            return result;
        }

        // 5. 保存文书
        DocumentDTO savedDocument = saveDocument(command, generatedContent);

        Map<String, Object> result = new HashMap<>();
        result.put("content", generatedContent);
        result.put("preview", false);
        result.put("document", savedDocument);
        return result;
    }

    /**
     * 仅预览 AI 生成的文书（不保存）
     */
    public String previewDocument(AiGenerateDocumentCommand command) {
        command.setPreviewOnly(true);
        Map<String, Object> result = generateDocument(command);
        return (String) result.get("content");
    }

    /**
     * 检查 AI 服务是否可用，并返回所有可用的 AI 模型列表
     */
    public Map<String, Object> checkAiStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取所有启用的 AI 集成
        java.util.List<ExternalIntegration> allIntegrations = integrationAppService.getAllEnabledAIIntegrations();
        
        if (allIntegrations == null || allIntegrations.isEmpty()) {
            result.put("available", false);
            result.put("message", "未配置或未启用 AI 大模型");
            result.put("models", java.util.List.of());
        } else {
            result.put("available", true);
            // 默认模型信息（第一个）
            ExternalIntegration defaultModel = allIntegrations.get(0);
            result.put("model", defaultModel.getIntegrationName());
            result.put("code", defaultModel.getIntegrationCode());
            result.put("defaultId", defaultModel.getId());
            
            // 所有可用模型列表
            java.util.List<Map<String, Object>> models = allIntegrations.stream()
                    .map(ai -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", ai.getId());
                        m.put("name", ai.getIntegrationName());
                        m.put("code", ai.getIntegrationCode());
                        m.put("description", ai.getDescription());
                        // 从 extraConfig 获取模型名称
                        if (ai.getExtraConfig() != null && ai.getExtraConfig().get("model") != null) {
                            m.put("modelName", ai.getExtraConfig().get("model"));
                        }
                        return m;
                    })
                    .toList();
            result.put("models", models);
        }
        
        return result;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(AiGenerateDocumentCommand command) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的法律文书撰写助手，拥有丰富的法律文书起草经验。\n\n");
        prompt.append("你的任务是根据用户的需求，撰写专业、规范、严谨的法律文书。\n\n");
        prompt.append("撰写要求：\n");
        prompt.append("1. 语言准确、专业，符合法律文书规范\n");
        prompt.append("2. 格式清晰，段落分明\n");
        prompt.append("3. 内容完整，逻辑严密\n");
        prompt.append("4. 引用法律条文要准确\n");
        prompt.append("5. 如有不确定的信息，用 [待补充] 标记\n\n");
        
        if (command.getTone() != null && !command.getTone().isEmpty()) {
            prompt.append("语气风格要求：").append(command.getTone()).append("\n\n");
        }
        
        prompt.append("请直接输出文书内容，不要添加额外说明。");
        
        return prompt.toString();
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(AiGenerateDocumentCommand command) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请撰写一份【").append(command.getDocumentType()).append("】\n\n");
        prompt.append("具体要求：\n").append(command.getRequirement()).append("\n\n");

        // 优先使用收集的项目上下文
        if (Boolean.TRUE.equals(command.getUseMatterContext()) && command.getMatterContext() != null) {
            prompt.append("===== 项目完整信息 =====\n");
            prompt.append(command.getMatterContext().toTextDescription());
            prompt.append("\n");
            
            // 标记是否已脱敏
            if (command.getMatterContext().isMasked()) {
                prompt.append("注意：以上信息已进行脱敏处理，带*的部分为脱敏内容，请在生成文书时保留脱敏格式。\n\n");
            }
        } 
        // 否则使用简单的项目信息获取（向后兼容）
        else if (command.getMatterId() != null) {
            Matter matter = matterRepository.findById(command.getMatterId());
            if (matter != null) {
                prompt.append("===== 项目信息 =====\n");
                prompt.append("项目名称：").append(matter.getName()).append("\n");
                prompt.append("项目编号：").append(matter.getMatterNo()).append("\n");
                if (matter.getMatterType() != null) {
                    prompt.append("项目类型：").append(matter.getMatterType()).append("\n");
                }
                // 获取客户名称
                if (matter.getClientId() != null) {
                    Client client = clientRepository.findById(matter.getClientId());
                    if (client != null && client.getName() != null) {
                        prompt.append("客户名称：").append(client.getName()).append("\n");
                    }
                }
                if (matter.getDescription() != null) {
                    prompt.append("项目描述：").append(matter.getDescription()).append("\n");
                }
                prompt.append("\n");
            }
        }

        // 添加当前用户信息（律师信息）
        Long userId = SecurityUtils.getUserId();
        if (userId != null) {
            User user = userRepository.findById(userId);
            if (user != null) {
                prompt.append("===== 律师信息 =====\n");
                prompt.append("律师姓名：").append(user.getRealName()).append("\n");
                prompt.append("\n");
            }
        }

        // 添加日期信息
        prompt.append("===== 其他信息 =====\n");
        prompt.append("当前日期：").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))).append("\n");

        // 额外上下文
        if (command.getAdditionalContext() != null && !command.getAdditionalContext().isEmpty()) {
            prompt.append("\n===== 补充信息 =====\n");
            prompt.append(command.getAdditionalContext()).append("\n");
        }

        return prompt.toString();
    }

    /**
     * 保存生成的文书
     */
    private DocumentDTO saveDocument(AiGenerateDocumentCommand command, String content) {
        // 生成文件名
        String fileName = command.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            fileName = command.getDocumentType() + "_" + 
                       LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "_" +
                       UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        }
        
        // 生成文档编号
        String docNo = "AI" + LocalDate.now().toString().replace("-", "").substring(2) +
                       UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        byte[] contentBytes = content.getBytes();
        
        // 上传文件到 MinIO
        String filePath;
        if (command.getMatterId() != null) {
            filePath = "documents/" + command.getMatterId() + "/" + fileName + ".docx";
        } else {
            filePath = "documents/personal/" + SecurityUtils.getUserId() + "/" + fileName + ".docx";
        }
        
        String fileUrl;
        try {
            fileUrl = minioService.uploadBytes(contentBytes, filePath, 
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (Exception e) {
            log.error("上传 AI 生成文书失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        // 创建文档记录
        Document document = Document.builder()
                .docNo(docNo)
                .title(fileName)
                .matterId(command.getMatterId()) // 可能为 null（个人文书）
                .fileName(fileName + ".docx")
                .filePath(fileUrl)
                .fileSize((long) contentBytes.length)
                .fileType("docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .version(1)
                .isLatest(true)
                .securityLevel("INTERNAL")
                .status("ACTIVE")
                .createdBy(SecurityUtils.getUserId())
                .aiGenerated(true) // 标记为 AI 生成
                .build();

        // 如果指定了卷宗目录，设置关联
        if (command.getDossierItemId() != null) {
            document.setDossierItemId(command.getDossierItemId());
        }

        documentRepository.save(document);

        log.info("AI 生成文书保存成功: docNo={}, matterId={}, fileName={}", 
                docNo, command.getMatterId(), fileName);

        return documentAppService.getDocumentById(document.getId());
    }
}

