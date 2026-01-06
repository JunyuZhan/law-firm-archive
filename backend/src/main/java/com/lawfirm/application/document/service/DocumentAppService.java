package com.lawfirm.application.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.document.command.CreateDocumentCommand;
import com.lawfirm.application.document.command.UpdateDocumentCommand;
import com.lawfirm.application.document.command.UploadNewVersionCommand;
import com.lawfirm.application.document.dto.DocumentDTO;
import com.lawfirm.application.document.dto.DocumentQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.entity.DocumentVersion;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import com.lawfirm.infrastructure.persistence.mapper.DocumentVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文档应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentAppService {

    private final DocumentRepository documentRepository;
    private final DocumentCategoryRepository categoryRepository;
    private final DocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;

    /**
     * 分页查询文档
     */
    public PageResult<DocumentDTO> listDocuments(DocumentQueryDTO query) {
        IPage<Document> page = documentMapper.selectDocumentPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getTitle(),
                query.getCategoryId(),
                query.getMatterId(),
                query.getSecurityLevel(),
                query.getStatus(),
                query.getFileType()
        );

        List<DocumentDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建文档
     */
    @Transactional
    public DocumentDTO createDocument(CreateDocumentCommand command) {
        // 生成文档编号
        String docNo = generateDocNo();

        Document document = Document.builder()
                .docNo(docNo)
                .title(command.getTitle())
                .categoryId(command.getCategoryId())
                .matterId(command.getMatterId())
                .fileName(command.getFileName())
                .filePath(command.getFilePath())
                .fileSize(command.getFileSize())
                .fileType(command.getFileType())
                .mimeType(command.getMimeType())
                .version(1)
                .isLatest(true)
                .securityLevel(command.getSecurityLevel() != null ? command.getSecurityLevel() : "INTERNAL")
                .stage(command.getStage())
                .tags(command.getTags())
                .description(command.getDescription())
                .status("ACTIVE")
                .build();

        documentRepository.save(document);

        // 记录版本历史
        saveVersion(document, "初始版本");

        log.info("文档创建成功: {} ({})", document.getTitle(), document.getDocNo());
        return toDTO(document);
    }

    /**
     * 更新文档信息
     */
    @Transactional
    public DocumentDTO updateDocument(UpdateDocumentCommand command) {
        Document document = documentRepository.getByIdOrThrow(command.getId(), "文档不存在");

        if (StringUtils.hasText(command.getTitle())) {
            document.setTitle(command.getTitle());
        }
        if (command.getCategoryId() != null) {
            document.setCategoryId(command.getCategoryId());
        }
        if (StringUtils.hasText(command.getSecurityLevel())) {
            document.setSecurityLevel(command.getSecurityLevel());
        }
        if (command.getStage() != null) {
            document.setStage(command.getStage());
        }
        if (command.getTags() != null) {
            document.setTags(command.getTags());
        }
        if (command.getDescription() != null) {
            document.setDescription(command.getDescription());
        }

        documentRepository.updateById(document);
        log.info("文档更新成功: {}", document.getTitle());
        return toDTO(document);
    }

    /**
     * 上传新版本
     */
    @Transactional
    public DocumentDTO uploadNewVersion(UploadNewVersionCommand command) {
        Document oldDoc = documentRepository.getByIdOrThrow(command.getDocumentId(), "文档不存在");

        // 将旧版本标记为非最新
        oldDoc.setIsLatest(false);
        documentRepository.updateById(oldDoc);

        // 创建新版本文档
        int newVersion = oldDoc.getVersion() + 1;
        Document newDoc = Document.builder()
                .docNo(oldDoc.getDocNo())
                .title(oldDoc.getTitle())
                .categoryId(oldDoc.getCategoryId())
                .matterId(oldDoc.getMatterId())
                .fileName(command.getFileName())
                .filePath(command.getFilePath())
                .fileSize(command.getFileSize())
                .fileType(command.getFileType() != null ? command.getFileType() : oldDoc.getFileType())
                .mimeType(command.getMimeType() != null ? command.getMimeType() : oldDoc.getMimeType())
                .version(newVersion)
                .isLatest(true)
                .parentDocId(oldDoc.getParentDocId() != null ? oldDoc.getParentDocId() : oldDoc.getId())
                .securityLevel(oldDoc.getSecurityLevel())
                .stage(oldDoc.getStage())
                .tags(oldDoc.getTags())
                .description(oldDoc.getDescription())
                .status("ACTIVE")
                .build();

        documentRepository.save(newDoc);

        // 记录版本历史
        saveVersion(newDoc, command.getChangeNote());

        log.info("文档新版本上传成功: {} v{}", newDoc.getTitle(), newVersion);
        return toDTO(newDoc);
    }

    /**
     * 获取文档详情
     */
    public DocumentDTO getDocumentById(Long id) {
        Document document = documentRepository.getByIdOrThrow(id, "文档不存在");
        return toDTO(document);
    }

    /**
     * 获取文档所有版本
     */
    public List<DocumentDTO> getDocumentVersions(Long id) {
        Document document = documentRepository.getByIdOrThrow(id, "文档不存在");
        Long rootId = document.getParentDocId() != null ? document.getParentDocId() : document.getId();
        
        List<Document> versions = documentRepository.findAllVersions(rootId);
        return versions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 回退到指定版本
     * @param documentId 当前文档ID
     * @param targetVersion 目标版本号
     */
    @Transactional
    public DocumentDTO rollbackToVersion(Long documentId, Integer targetVersion) {
        Document currentDoc = documentRepository.getByIdOrThrow(documentId, "文档不存在");
        
        // 获取根文档ID
        Long rootId = currentDoc.getParentDocId() != null ? currentDoc.getParentDocId() : currentDoc.getId();
        
        // 查找目标版本
        List<Document> allVersions = documentRepository.findAllVersions(rootId);
        Document targetDoc = allVersions.stream()
                .filter(d -> d.getVersion().equals(targetVersion))
                .findFirst()
                .orElseThrow(() -> new BusinessException("目标版本不存在: v" + targetVersion));
        
        if (targetDoc.getVersion() >= currentDoc.getVersion()) {
            throw new BusinessException("只能回退到更早的版本");
        }
        
        // 将当前最新版本标记为非最新
        Document latestDoc = allVersions.stream()
                .filter(Document::getIsLatest)
                .findFirst()
                .orElse(currentDoc);
        latestDoc.setIsLatest(false);
        documentRepository.updateById(latestDoc);
        
        // 创建新版本（基于目标版本的内容）
        int newVersion = latestDoc.getVersion() + 1;
        Document rollbackDoc = Document.builder()
                .docNo(targetDoc.getDocNo())
                .title(targetDoc.getTitle())
                .categoryId(targetDoc.getCategoryId())
                .matterId(targetDoc.getMatterId())
                .fileName(targetDoc.getFileName())
                .filePath(targetDoc.getFilePath())
                .fileSize(targetDoc.getFileSize())
                .fileType(targetDoc.getFileType())
                .mimeType(targetDoc.getMimeType())
                .version(newVersion)
                .isLatest(true)
                .parentDocId(rootId)
                .securityLevel(targetDoc.getSecurityLevel())
                .stage(targetDoc.getStage())
                .tags(targetDoc.getTags())
                .description(targetDoc.getDescription())
                .status("ACTIVE")
                .build();
        
        documentRepository.save(rollbackDoc);
        
        // 记录版本历史
        saveVersion(rollbackDoc, "回退到版本 v" + targetVersion);
        
        log.info("文档版本回退成功: {} v{} -> v{} (新版本号: v{})", 
                rollbackDoc.getTitle(), latestDoc.getVersion(), targetVersion, newVersion);
        return toDTO(rollbackDoc);
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document document = documentRepository.getByIdOrThrow(id, "文档不存在");
        document.setStatus("DELETED");
        documentRepository.updateById(document);
        log.info("文档删除成功: {}", document.getTitle());
    }

    /**
     * 归档文档
     */
    @Transactional
    public void archiveDocument(Long id) {
        Document document = documentRepository.getByIdOrThrow(id, "文档不存在");
        document.setStatus("ARCHIVED");
        documentRepository.updateById(document);
        log.info("文档归档成功: {}", document.getTitle());
    }

    /**
     * 按案件查询文档
     */
    public List<DocumentDTO> getDocumentsByMatter(Long matterId) {
        List<Document> documents = documentRepository.list(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getMatterId, matterId)
                        .eq(Document::getIsLatest, true)
                        .eq(Document::getDeleted, false)
                        .orderByDesc(Document::getCreatedAt)
        );
        return documents.stream().map(this::toDTO).collect(Collectors.toList());
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
     * 保存版本历史
     */
    private void saveVersion(Document document, String changeNote) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            log.warn("获取当前用户ID失败，使用null: {}", e.getMessage());
        }
        
        DocumentVersion version = DocumentVersion.builder()
                .documentId(document.getId())
                .version(document.getVersion())
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .changeNote(changeNote)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        versionMapper.insert(version);
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
     * 获取安全级别名称
     */
    private String getSecurityLevelName(String level) {
        if (level == null) return null;
        return switch (level) {
            case "PUBLIC" -> "公开";
            case "INTERNAL" -> "内部";
            case "CONFIDENTIAL" -> "机密";
            case "TOP_SECRET" -> "绝密";
            default -> level;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "ACTIVE" -> "正常";
            case "ARCHIVED" -> "已归档";
            case "DELETED" -> "已删除";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private DocumentDTO toDTO(Document doc) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setDocNo(doc.getDocNo());
        dto.setTitle(doc.getTitle());
        dto.setCategoryId(doc.getCategoryId());
        if (doc.getCategoryId() != null) {
            DocumentCategory category = categoryRepository.findById(doc.getCategoryId());
            if (category != null) {
                dto.setCategoryName(category.getName());
            }
        }
        dto.setMatterId(doc.getMatterId());
        dto.setFileName(doc.getFileName());
        dto.setFilePath(doc.getFilePath());
        dto.setFileSize(doc.getFileSize());
        dto.setFileSizeDisplay(formatFileSize(doc.getFileSize()));
        dto.setFileType(doc.getFileType());
        dto.setMimeType(doc.getMimeType());
        dto.setVersion(doc.getVersion());
        dto.setIsLatest(doc.getIsLatest());
        dto.setParentDocId(doc.getParentDocId());
        dto.setSecurityLevel(doc.getSecurityLevel());
        dto.setSecurityLevelName(getSecurityLevelName(doc.getSecurityLevel()));
        dto.setStage(doc.getStage());
        dto.setTags(doc.getTags());
        dto.setDescription(doc.getDescription());
        dto.setStatus(doc.getStatus());
        dto.setStatusName(getStatusName(doc.getStatus()));
        dto.setCreatedBy(doc.getCreatedBy());
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setUpdatedAt(doc.getUpdatedAt());
        return dto;
    }
}
