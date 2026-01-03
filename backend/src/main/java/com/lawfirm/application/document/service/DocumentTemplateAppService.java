package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateDocumentTemplateCommand;
import com.lawfirm.application.document.dto.DocumentTemplateDTO;
import com.lawfirm.application.document.dto.DocumentTemplateQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.application.document.command.BatchGenerateDocumentCommand;
import com.lawfirm.application.document.command.GenerateDocumentCommand;
import com.lawfirm.application.document.command.PreviewTemplateCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.service.DocumentAppService;
import com.lawfirm.application.document.service.TemplateVariableService;
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
    public DocumentTemplateDTO updateTemplate(Long id, String name, Long categoryId, String templateType,
                                              String description, String status) {
        DocumentTemplate template = templateRepository.getByIdOrThrow(id, "模板不存在");

        if (StringUtils.hasText(name)) {
            template.setName(name);
        }
        if (categoryId != null) {
            template.setCategoryId(categoryId);
        }
        if (StringUtils.hasText(templateType)) {
            template.setTemplateType(templateType);
        }
        if (description != null) {
            template.setDescription(description);
        }
        if (StringUtils.hasText(status)) {
            template.setStatus(status);
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

        // 读取模板文件
        byte[] templateBytes;
        try {
            String objectName = minioService.extractObjectName(template.getFilePath());
            if (objectName == null) {
                objectName = template.getFilePath();
            }
            templateBytes = minioService.downloadFileAsBytes(objectName);
        } catch (Exception e) {
            log.error("读取模板文件失败: {}", template.getFilePath(), e);
            throw new BusinessException("模板文件读取失败: " + e.getMessage());
        }

        // 替换变量（简化版：仅处理文本替换，实际应该使用Apache POI处理Word文档）
        String templateContent = new String(templateBytes);
        String generatedContent = templateVariableService.replaceVariables(templateContent, variables);
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

        // 如果模板文件存在，可以读取并预览替换后的内容（简化版）
        try {
            String objectName = minioService.extractObjectName(template.getFilePath());
            if (objectName == null) {
                objectName = template.getFilePath();
            }
            byte[] templateBytes = minioService.downloadFileAsBytes(objectName);
            if (templateBytes != null) {
                String templateContent = new String(templateBytes);
                String previewContent = templateVariableService.replaceVariables(templateContent, variableValues);
                result.put("previewContent", previewContent);
            }
        } catch (Exception e) {
            log.warn("读取模板文件失败: {}", template.getFilePath(), e);
        }

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
            case "POWER_OF_ATTORNEY" -> "授权委托书";
            case "COMPLAINT" -> "起诉状";
            case "DEFENSE" -> "答辩状";
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
