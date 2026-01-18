package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateDocumentTemplateCommand;
import com.lawfirm.application.document.command.UpdateDocumentTemplateCommand;
import com.lawfirm.application.document.dto.DocumentTemplateDTO;
import com.lawfirm.application.document.dto.DocumentTemplateQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.application.document.command.BatchGenerateDocumentCommand;
import com.lawfirm.application.document.command.GenerateDocumentCommand;
import com.lawfirm.application.document.command.PreviewTemplateCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.entity.DocumentTemplate;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.domain.document.repository.DocumentTemplateRepository;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.persistence.mapper.DocumentTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档模板应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTemplateAppService {

    private final DocumentTemplateRepository templateRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final DocumentTemplateMapper templateMapper;
    private final TemplateVariableService templateVariableService;
    private final DocumentAppService documentAppService;
    private final DocumentRepository documentRepository;
    private final MinioService minioService;

    /**
     * 分页查询模板
     */
    public PageResult<DocumentTemplateDTO> listTemplates(DocumentTemplateQueryDTO query) {
        IPage<DocumentTemplate> page = templateMapper.selectTemplatePage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getName(),
                query.getCategoryId(),
                query.getTemplateType(),
                query.getStatus()
        );

        List<DocumentTemplateDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建模板
     */
    @Transactional
    public DocumentTemplateDTO createTemplate(CreateDocumentTemplateCommand command) {
        String templateNo = generateTemplateNo();

        DocumentTemplate template = DocumentTemplate.builder()
                .templateNo(templateNo)
                .name(command.getName())
                .categoryId(command.getCategoryId())
                .templateType(command.getTemplateType())
                .fileName(command.getFileName())
                .filePath(command.getFilePath())
                .fileSize(command.getFileSize())
                .variables(command.getVariables())
                .description(command.getDescription())
                .status("ACTIVE")
                .useCount(0)
                .build();

        templateRepository.save(template);
        log.info("文档模板创建成功: {} ({})", template.getName(), template.getTemplateNo());
        return toDTO(template);
    }

    /**
     * 获取模板详情
     */
    public DocumentTemplateDTO getTemplateById(Long id) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
        return toDTO(template);
    }

    /**
     * 更新模板
     */
    @Transactional
    public DocumentTemplateDTO updateTemplate(Long id, UpdateDocumentTemplateCommand command) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");

        if (StringUtils.hasText(command.getName())) {
            template.setName(command.getName());
        }
        if (command.getCategoryId() != null) {
            template.setCategoryId(command.getCategoryId());
        }
        if (StringUtils.hasText(command.getTemplateType())) {
            template.setTemplateType(command.getTemplateType());
        }
        if (command.getDescription() != null) {
            template.setDescription(command.getDescription());
        }
        if (command.getContent() != null) {
            template.setContent(command.getContent());
        }
        if (StringUtils.hasText(command.getStatus())) {
            template.setStatus(command.getStatus());
        }

        templateRepository.updateById(template);
        log.info("文档模板更新成功: {}", template.getName());
        return toDTO(template);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long id) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
        templateRepository.removeById(id);
        log.info("文档模板删除成功: {}", template.getName());
    }

    /**
     * 使用模板（增加使用次数）
     */
    @Transactional
    public void useTemplate(Long id) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("模板已停用");
        }
        templateRepository.incrementUseCount(id);
        log.info("模板使用次数+1: {}", template.getName());
    }

    /**
     * 从模板生成文档（M5-033）
     */
    @Transactional
    public DocumentDTO generateDocument(GenerateDocumentCommand command) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(command.getTemplateId(), "模板不存在");
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("模板已停用");
        }

        // 收集变量值
        Map<String, Object> variables = templateVariableService.collectVariables(command.getMatterId());
        if (command.getExtraVariables() != null) {
            variables.putAll(command.getExtraVariables());
        }

        // 获取模板内容：优先使用数据库中的 content 字段，其次尝试从文件读取
        String generatedContent = null;
        
        // 1. 优先使用数据库中存储的模板内容
        if (template.getContent() != null && !template.getContent().isEmpty()) {
            generatedContent = templateVariableService.replaceVariables(template.getContent(), variables);
        } else {
            // 2. 尝试从文件读取模板内容
        try {
            String objectName = minioService.extractObjectName(template.getFilePath());
            if (objectName == null) {
                objectName = template.getFilePath();
            }
                byte[] templateBytes = minioService.downloadFileAsBytes(objectName);
                if (templateBytes != null) {
                    String templateContent = new String(templateBytes);
                    generatedContent = templateVariableService.replaceVariables(templateContent, variables);
                }
        } catch (Exception e) {
                log.warn("读取模板文件失败: {}", template.getFilePath());
            }
        }
        
        // 3. 如果都没有内容，抛出异常
        if (generatedContent == null || generatedContent.isEmpty()) {
            throw new BusinessException("模板内容为空，请先编辑模板内容");
        }

        // 解码 HTML 实体（防止 XSS 过滤导致的格式问题，如 &lt;、&gt; 等）
        generatedContent = decodeHtmlEntities(generatedContent);

        byte[] generatedBytes = generatedContent.getBytes();

        // 生成文档名称
        String documentName = command.getDocumentName();
        if (documentName == null || documentName.isEmpty()) {
            documentName = template.getName() + "-" + System.currentTimeMillis();
        }

        // 上传生成的文件
        String fileName = documentName + ".docx";
        String filePath = "documents/" + command.getMatterId() + "/" + fileName;
        String fileUrl;
        try {
            fileUrl = minioService.uploadBytes(generatedBytes, filePath, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        } catch (Exception e) {
            log.error("上传生成的文件失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        // 创建文档记录
        Document document = Document.builder()
                .docNo(generateDocNo())
                .title(documentName)
                .categoryId(template.getCategoryId())
                .matterId(command.getMatterId())
                .fileName(fileName)
                .filePath(fileUrl)
                .fileSize((long) generatedBytes.length)
                .fileType("docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .version(1)
                .isLatest(true)
                .securityLevel("INTERNAL")
                .status("ACTIVE")
                .createdBy(SecurityUtils.getUserId())
                .build();

        // 如果指定了卷宗目录，设置关联
        if (command.getDossierItemId() != null) {
            document.setDossierItemId(command.getDossierItemId());
        }

        documentRepository.save(document);

        // 增加模板使用次数
        templateRepository.incrementUseCount(command.getTemplateId());

        log.info("从模板生成文档成功: templateId={}, matterId={}, documentId={}", 
                command.getTemplateId(), command.getMatterId(), document.getId());
        return documentAppService.getDocumentById(document.getId());
    }

    /**
     * 批量生成文档（M5-034）
     */
    @Transactional
    public List<DocumentDTO> batchGenerateDocuments(BatchGenerateDocumentCommand command) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(command.getTemplateId(), "模板不存在");
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("模板已停用");
        }

        List<DocumentDTO> generatedDocuments = new java.util.ArrayList<>();

        for (Long matterId : command.getMatterIds()) {
            try {
                // 为每个案件生成文档
                GenerateDocumentCommand generateCommand = new GenerateDocumentCommand();
                generateCommand.setTemplateId(command.getTemplateId());
                generateCommand.setMatterId(matterId);
                
                // 处理文档名称模板
                if (command.getDocumentNameTemplate() != null && !command.getDocumentNameTemplate().isEmpty()) {
                    Map<String, Object> variables = templateVariableService.collectVariables(matterId);
                    String documentName = templateVariableService.replaceVariables(command.getDocumentNameTemplate(), variables);
                    generateCommand.setDocumentName(documentName);
                }
                
                generateCommand.setExtraVariables(command.getExtraVariables());
                
                DocumentDTO document = generateDocument(generateCommand);
                generatedDocuments.add(document);
            } catch (Exception e) {
                log.error("批量生成文档失败: matterId={}", matterId, e);
                // 继续处理下一个案件
            }
        }

        log.info("批量生成文档完成: templateId={}, total={}, success={}", 
                command.getTemplateId(), command.getMatterIds().size(), generatedDocuments.size());
        return generatedDocuments;
    }

    /**
     * 预览模板（M5-035）
     */
    public Map<String, Object> previewTemplate(PreviewTemplateCommand command) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(command.getTemplateId(), "模板不存在");

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("templateId", template.getId());
        result.put("templateName", template.getName());
        result.put("variables", template.getVariables());

        // 收集变量值
        Map<String, Object> variableValues = new java.util.HashMap<>();
        if (command.getPreviewVariables() != null && !command.getPreviewVariables().isEmpty()) {
            // 使用提供的预览变量
            variableValues.putAll(command.getPreviewVariables());
        } else if (command.getMatterId() != null) {
            // 从案件收集变量
            variableValues.putAll(templateVariableService.collectVariables(command.getMatterId()));
        } else {
            // 使用默认值
            variableValues.put("matter.name", "示例案件名称");
            variableValues.put("matter.no", "MAT20240101001");
            variableValues.put("client.name", "示例客户名称");
            variableValues.put("lawyer.name", "示例律师姓名");
            variableValues.put("date.today", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        }

        result.put("variableValues", variableValues);

        // 获取模板内容：优先使用数据库中的 content 字段，其次尝试从文件读取
        String previewContent = null;
        
        // 1. 优先使用数据库中存储的模板内容
        if (template.getContent() != null && !template.getContent().isEmpty()) {
            previewContent = templateVariableService.replaceVariables(template.getContent(), variableValues);
        } else {
            // 2. 尝试从文件读取模板内容
        try {
            String objectName = minioService.extractObjectName(template.getFilePath());
            if (objectName == null) {
                objectName = template.getFilePath();
            }
            byte[] templateBytes = minioService.downloadFileAsBytes(objectName);
            if (templateBytes != null) {
                String templateContent = new String(templateBytes);
                    previewContent = templateVariableService.replaceVariables(templateContent, variableValues);
            }
        } catch (Exception e) {
                log.warn("读取模板文件失败: {}", template.getFilePath());
            }
        }

        // 3. 如果都没有，返回提示信息
        if (previewContent == null) {
            previewContent = "【" + template.getName() + "】\n\n模板内容为空，请在模板管理中编辑模板内容。";
        }

        result.put("previewContent", previewContent);
        return result;
    }

    /**
     * 生成文档编号
     */
    private String generateDocNo() {
        String prefix = "DOC" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 生成模板编号
     */
    private String generateTemplateNo() {
        String prefix = "TPL" + LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + random;
    }

    /**
     * 解码 HTML 实体
     * 用于修复 XSS 拦截导致的 HTML 实体转义问题（如 &lt;、&gt;、&quot; 等）
     * 
     * @param text 需要解码的文本
     * @return 解码后的文本
     */
    private String decodeHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        String result = text;
        
        // 常见 HTML 实体映射
        result = result.replace("&quot;", "\"");
        result = result.replace("&amp;", "&");
        result = result.replace("&lt;", "<");
        result = result.replace("&gt;", ">");
        result = result.replace("&apos;", "'");
        result = result.replace("&#39;", "'");
        result = result.replace("&#x27;", "'");
        result = result.replace("&nbsp;", " ");
        
        // 处理数字实体 &#xxx;
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("&#(\\d+);");
        java.util.regex.Matcher matcher1 = pattern1.matcher(result);
        StringBuffer sb1 = new StringBuffer();
        while (matcher1.find()) {
            int code = Integer.parseInt(matcher1.group(1));
            matcher1.appendReplacement(sb1, String.valueOf((char) code));
        }
        matcher1.appendTail(sb1);
        result = sb1.toString();
        
        // 处理十六进制实体 &#xXXX;
        java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("&#x([0-9a-fA-F]+);");
        java.util.regex.Matcher matcher2 = pattern2.matcher(result);
        StringBuffer sb2 = new StringBuffer();
        while (matcher2.find()) {
            int code = Integer.parseInt(matcher2.group(1), 16);
            matcher2.appendReplacement(sb2, String.valueOf((char) code));
        }
        matcher2.appendTail(sb2);
        result = sb2.toString();
        
        return result;
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(Long size) {
        if (size == null) return null;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * 获取模板类型名称
     */
    private String getTemplateTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "CONTRACT" -> "合同";
            case "LEGAL_OPINION" -> "法律意见书";
            case "POWER_OF_ATTORNEY" -> "授权委托书(自动归档)";
            case "COMPLAINT" -> "起诉状";
            case "DEFENSE" -> "答辩状";
            case "WORD" -> "Word文档";
            case "EXCEL" -> "Excel表格";
            case "PDF" -> "PDF文档";
            case "HTML" -> "富文本";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    /**
     * Entity 转 DTO
     */
    private DocumentTemplateDTO toDTO(DocumentTemplate template) {
        DocumentTemplateDTO dto = new DocumentTemplateDTO();
        dto.setId(template.getId());
        dto.setTemplateNo(template.getTemplateNo());
        dto.setName(template.getName());
        dto.setCategoryId(template.getCategoryId());
        if (template.getCategoryId() != null) {
            DocumentCategory category = categoryRepository.findById(template.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
        }
        dto.setTemplateType(template.getTemplateType());
        dto.setTemplateTypeName(getTemplateTypeName(template.getTemplateType()));
        dto.setFileName(template.getFileName());
        dto.setFilePath(template.getFilePath());
        dto.setFileSize(template.getFileSize());
        dto.setFileSizeDisplay(formatFileSize(template.getFileSize()));
        dto.setVariables(template.getVariables());
        dto.setDescription(template.getDescription());
        dto.setStatus(template.getStatus());
        dto.setStatusName("ACTIVE".equals(template.getStatus()) ? "启用" : "停用");
        dto.setUseCount(template.getUseCount());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        return dto;
    }
}
