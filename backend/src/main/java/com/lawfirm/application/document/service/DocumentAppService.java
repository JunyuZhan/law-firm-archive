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
import com.lawfirm.common.util.FileValidator;
import com.lawfirm.common.util.FileHashUtil;
import com.lawfirm.common.util.MinioPathGenerator;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.domain.document.entity.DocumentCategory;
import com.lawfirm.domain.document.entity.DocumentVersion;
import com.lawfirm.domain.document.repository.DocumentCategoryRepository;
import com.lawfirm.domain.document.repository.DocumentRepository;
import com.lawfirm.infrastructure.persistence.mapper.DocumentMapper;
import com.lawfirm.infrastructure.persistence.mapper.DocumentVersionMapper;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.external.file.ThumbnailService;
import com.lawfirm.application.matter.service.MatterAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
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
    private final MinioService minioService;
    private final ThumbnailService thumbnailService;
    private MatterAppService matterAppService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @Lazy
    public void setMatterAppService(MatterAppService matterAppService) {
        this.matterAppService = matterAppService;
    }

    /**
     * 分页查询文档
     */
    public PageResult<DocumentDTO> listDocuments(DocumentQueryDTO query) {
        // 根据用户权限过滤数据
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        
        // 获取可访问的项目ID列表
        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        // 如果返回空列表，表示没有权限，返回空结果
        if (accessibleMatterIds != null && accessibleMatterIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
        }
        
        // 如果query中指定了matterId，需要验证是否有权限访问该项目
        if (query.getMatterId() != null && accessibleMatterIds != null) {
            if (!accessibleMatterIds.contains(query.getMatterId())) {
                // 没有权限访问指定的项目，返回空结果
                return PageResult.of(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
            }
        }
        
        IPage<Document> page = documentMapper.selectDocumentPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getTitle(),
                query.getCategoryId(),
                query.getMatterId(),
                query.getSecurityLevel(),
                query.getStatus(),
                query.getFileType(),
                query.getCreatedBy(),
                accessibleMatterIds  // null表示可以访问所有项目的文档（ALL权限）
        );

        // 批量转换DTO（避免N+1查询）
        List<DocumentDTO> records = batchConvertToDTO(page.getRecords());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }
    
    /**
     * 批量转换文档列表为DTO（性能优化：避免N+1查询）
     */
    private List<DocumentDTO> batchConvertToDTO(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 收集所有需要查询的分类ID
        Set<Long> categoryIds = documents.stream()
                .map(Document::getCategoryId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        
        // 批量加载分类信息
        Map<Long, DocumentCategory> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
                categoryRepository.listByIds(new ArrayList<>(categoryIds)).stream()
                        .collect(Collectors.toMap(DocumentCategory::getId, c -> c, (a, b) -> a));
        
        // 使用预加载的数据转换DTO
        return documents.stream()
                .map(doc -> toDTOWithMap(doc, categoryMap))
                .collect(Collectors.toList());
    }
    
    /**
     * 使用预加载的Map转换单个文档DTO（避免N+1查询）
     */
    private DocumentDTO toDTOWithMap(Document doc, Map<Long, DocumentCategory> categoryMap) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(doc.getId());
        dto.setDocNo(doc.getDocNo());
        dto.setTitle(doc.getTitle());
        dto.setCategoryId(doc.getCategoryId());
        if (doc.getCategoryId() != null) {
            DocumentCategory category = categoryMap.get(doc.getCategoryId());
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
        dto.setDossierItemId(doc.getDossierItemId());
        dto.setFolderPath(doc.getFolderPath());
        dto.setDisplayOrder(doc.getDisplayOrder());
        // 转换缩略图 URL 为浏览器可访问的 URL
        String thumbnailUrl = doc.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            thumbnailUrl = minioService.getBrowserAccessibleUrl(thumbnailUrl);
        }
        dto.setThumbnailUrl(thumbnailUrl);
        return dto;
    }

    /**
     * 创建文档
     */
    @Transactional
    public DocumentDTO createDocument(CreateDocumentCommand command) {
        // 验证用户是否是项目负责人或参与者（只有项目成员才能上传文档）
        if (command.getMatterId() != null) {
            matterAppService.validateMatterOwnership(command.getMatterId());
        }
        
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
        
        // 验证用户是否是项目负责人或参与者（只有项目成员才能编辑文档）
        if (document.getMatterId() != null) {
            matterAppService.validateMatterOwnership(document.getMatterId());
        }

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
        
        // 验证用户是否是项目负责人或参与者（只有项目成员才能上传新版本）
        if (oldDoc.getMatterId() != null) {
            matterAppService.validateMatterOwnership(oldDoc.getMatterId());
        }

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
     * 获取文档实体（用于FileAccessService等需要实体对象的场景）
     */
    public Document getDocumentEntityById(Long id) {
        return documentRepository.getByIdOrThrow(id, "文档不存在");
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
     * 删除文档（软删除）
     * 使用 MyBatis-Plus 的 removeById 来正确处理 @TableLogic 注解
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document document = documentRepository.getByIdOrThrow(id, "文档不存在");
        String title = document.getTitle();
        // 使用 softDelete 方法，它会调用 MyBatis-Plus 的 removeById
        // 这会自动将 deleted 字段设置为 true
        documentRepository.softDelete(id);
        log.info("文档删除成功: id={}, title={}", id, title);
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
     * 
     * 权限：继承项目的查看权限
     */
    public List<DocumentDTO> getDocumentsByMatter(Long matterId) {
        // 验证用户是否有权访问该项目
        String dataScope = SecurityUtils.getDataScope();
        Long currentUserId = SecurityUtils.getUserId();
        Long deptId = SecurityUtils.getDepartmentId();
        List<Long> accessibleMatterIds = matterAppService.getAccessibleMatterIds(dataScope, currentUserId, deptId);
        
        // null 表示可以访问所有项目（ALL权限）
        if (accessibleMatterIds != null && !accessibleMatterIds.contains(matterId)) {
            throw new BusinessException("无权访问该项目的文档");
        }
        
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
     * 上传单个文件
     */
    @Transactional
    public DocumentDTO uploadFile(MultipartFile file, Long matterId, String folder, String description, Long dossierItemId) {
        return uploadFile(file, matterId, folder, description, dossierItemId, null);
    }

    /**
     * 上传单个文件（支持指定来源类型）
     */
    @Transactional
    public DocumentDTO uploadFile(MultipartFile file, Long matterId, String folder, String description, Long dossierItemId, String sourceType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        
        // ✅ 安全验证：使用 FileValidator 验证文件
        FileValidator.ValidationResult validationResult = FileValidator.validate(file);
        if (!validationResult.isValid()) {
            log.warn("文件验证失败: {}, 原因: {}", file.getOriginalFilename(), validationResult.getErrorMessage());
            throw new BusinessException(validationResult.getErrorMessage());
        }
        
        try {
            // 1. 计算文件Hash（用于去重和校验，不阻塞上传）
            String fileHash = FileHashUtil.calculateHash(file);
            log.debug("文件Hash计算完成: hash={}, fileName={}", fileHash, file.getOriginalFilename());
            
            // 2. 生成标准化存储路径（新规则）
            String standardStoragePath = MinioPathGenerator.generateStandardPath(
                MinioPathGenerator.FileType.MATTERS, matterId, folder);
            
            // 2.1 验证存储路径格式（确保包含项目ID）
            MinioPathGenerator.validateStoragePath(standardStoragePath, 
                MinioPathGenerator.FileType.MATTERS, matterId);
            
            // 3. 生成物理文件名（包含时间戳和UUID）
            String originalFilename = file.getOriginalFilename();
            String physicalName = MinioPathGenerator.generatePhysicalName(originalFilename);
            
            // 4. 构建完整对象名称（新路径）
            String objectName = MinioPathGenerator.buildObjectName(standardStoragePath, physicalName);
            
            // 5. 上传文件到 MinIO（使用新路径）
            String newFileUrl = minioService.uploadFile(
                file.getInputStream(), 
                objectName, 
                file.getContentType()
            );
            
            // 6. 构建完整URL（用于file_path字段，双写策略）
            // 注意：file_path也指向新路径的URL，确保新旧字段指向同一文件
            String fileUrl = minioService.buildFileUrl(objectName);
            
            // 7. 获取文件信息
            String fileType = getFileExtension(originalFilename);
            String mimeType = file.getContentType();
            long fileSize = file.getSize();
            
            // 8. 生成缩略图（支持图片和PDF）
            String thumbnailUrl = null;
            if (thumbnailService.supportsThumbnail(originalFilename)) {
                // 使用新路径生成缩略图
                thumbnailUrl = thumbnailService.generateThumbnail(file, newFileUrl);
            }
            
            // 9. 确定来源类型（默认为用户上传）
            String actualSourceType = sourceType != null && !sourceType.isEmpty() 
                ? sourceType : "USER_UPLOADED";
            
            // 10. 创建文档记录（双写策略：新字段 + file_path）
            Document document = Document.builder()
                    .docNo(generateDocNo())
                    .title(originalFilename)
                    .matterId(matterId)
                    .fileName(originalFilename)
                    // 新字段（结构化存储）
                    .bucketName(minioService.getBucketName())
                    .storagePath(standardStoragePath)
                    .physicalName(physicalName)
                    .fileHash(fileHash)
                    // 旧字段（兼容，双写）- 指向新路径的URL
                    .filePath(fileUrl)
                    // 其他字段
                    .fileSize(fileSize)
                    .fileType(fileType)
                    .mimeType(mimeType)
                    .version(1)
                    .isLatest(true)
                    .securityLevel("INTERNAL")
                    .status("ACTIVE")
                    .description(description)
                    .dossierItemId(dossierItemId)
                    .folderPath(folder)
                    .thumbnailUrl(thumbnailUrl)
                    .sourceType(actualSourceType)
                    .createdBy(SecurityUtils.getUserId())
                    .build();
            
            documentRepository.save(document);
            
            // 11. 保存版本历史
            saveVersion(document, "初始版本");
            
            log.info("文件上传成功（新路径）: fileName={}, storagePath={}, physicalName={}, hash={}, fileUrl={}", 
                originalFilename, standardStoragePath, physicalName, fileHash, fileUrl);
            return toDTO(document);
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 批量上传文件（带事务补偿）
     * 
     * 事务策略：
     * 1. 先验证所有文件
     * 2. 批量上传到MinIO，记录已上传的文件URL
     * 3. 批量创建数据库记录
     * 4. 如果任一步骤失败，清理已上传的MinIO文件
     */
    @Transactional(rollbackFor = Exception.class)
    public List<DocumentDTO> uploadFiles(MultipartFile[] files, Long matterId, String folder, String description, Long dossierItemId, String sourceType) {
        if (files == null || files.length == 0) {
            throw new BusinessException("请选择要上传的文件");
        }
        
        // 收集有效文件
        List<MultipartFile> validFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                validFiles.add(file);
            }
        }
        
        if (validFiles.isEmpty()) {
            throw new BusinessException("没有有效的文件");
        }
        
        // 记录已上传到MinIO的文件路径，用于失败时清理
        List<String> uploadedFilePaths = new ArrayList<>();
        List<DocumentDTO> results = new ArrayList<>();
        
        try {
            // 逐个上传（上传时会自动创建数据库记录）
            for (MultipartFile file : validFiles) {
                DocumentDTO dto = uploadFile(file, matterId, folder, description, dossierItemId, sourceType);
                uploadedFilePaths.add(dto.getFilePath());
                results.add(dto);
            }
            
            log.info("批量上传完成: {} 个文件", results.size());
            return results;
            
        } catch (Exception e) {
            // 上传失败，清理已上传到MinIO的文件
            log.error("批量上传失败，开始清理已上传的文件: {} 个", uploadedFilePaths.size(), e);
            
            for (String filePath : uploadedFilePaths) {
                try {
                    if (filePath != null && !filePath.isEmpty()) {
                        // 从URL提取objectName（deleteFile需要objectName，不是URL）
                        String objectName = minioService.extractObjectName(filePath);
                        if (objectName != null && !objectName.isEmpty()) {
                            minioService.deleteFile(objectName);
                            log.info("清理失败上传的文件: {}", objectName);
                        } else {
                            // 如果无法提取objectName，尝试直接使用filePath（可能是objectName格式）
                            minioService.deleteFile(filePath);
                            log.info("清理失败上传的文件（直接使用路径）: {}", filePath);
                        }
                    }
                } catch (Exception cleanEx) {
                    log.error("清理文件失败: {}", filePath, cleanEx);
                }
            }
            
            // 重新抛出异常，触发事务回滚
            throw new BusinessException("批量上传失败: " + e.getMessage());
        }
    }

    /**
     * 构建存储路径
     */
    private String buildStoragePath(Long matterId, String folder) {
        StringBuilder path = new StringBuilder();
        
        if (matterId != null) {
            path.append("matters/").append(matterId).append("/");
        } else {
            path.append("personal/").append(SecurityUtils.getUserId()).append("/");
        }
        
        if (folder != null && !folder.isEmpty() && !"root".equals(folder)) {
            path.append(folder).append("/");
        }
        
        return path.toString();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
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
     * 保存编辑历史（用于 OnlyOffice 自动保存，不作为新版本）
     * 记录文件路径变化，但版本号保持不变
     */
    private void saveEditHistory(Document document, String changeNote) {
        Long userId = null;
        try {
            userId = SecurityUtils.getUserId();
        } catch (Exception e) {
            log.debug("OnlyOffice 回调获取用户ID失败，这是正常的: {}", e.getMessage());
        }
        
        // 记录编辑历史，版本号不变，但文件路径可能变化
        DocumentVersion editRecord = DocumentVersion.builder()
                .documentId(document.getId())
                .version(document.getVersion())  // 版本号不变
                .fileName(document.getFileName())
                .filePath(document.getFilePath())
                .fileSize(document.getFileSize())
                .changeNote(changeNote)
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        versionMapper.insert(editRecord);
        
        log.debug("OnlyOffice 编辑历史已记录: docId={}, version={}", 
                document.getId(), document.getVersion());
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
     * 移动文件到指定目录
     * @param documentId 文档ID
     * @param targetDossierItemId 目标卷宗目录项ID
     * @return 更新后的文档DTO
     */
    @Transactional
    public DocumentDTO moveDocument(Long documentId, Long targetDossierItemId) {
        Document document = documentRepository.getById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }
        
        // 验证用户权限
        if (document.getMatterId() != null) {
            matterAppService.validateMatterOwnership(document.getMatterId());
        }
        
        // 更新文档的目录关联
        Long oldDossierItemId = document.getDossierItemId();
        document.setDossierItemId(targetDossierItemId);
        documentRepository.updateById(document);
        
        // 更新原目录的文件计数
        if (oldDossierItemId != null) {
            updateDossierItemCount(oldDossierItemId, -1);
        }
        
        // 更新目标目录的文件计数
        if (targetDossierItemId != null) {
            updateDossierItemCount(targetDossierItemId, 1);
        }
        
        log.info("文档移动成功: docId={}, from={}, to={}", documentId, oldDossierItemId, targetDossierItemId);
        return toDTO(document);
    }
    
    /**
     * 更新卷宗目录项的文件计数
     */
    private void updateDossierItemCount(Long dossierItemId, int delta) {
        // 通过原生SQL更新计数，避免循环依赖
        try {
            documentMapper.updateDossierItemDocCount(dossierItemId, delta);
        } catch (Exception e) {
            log.warn("更新目录文件计数失败: itemId={}, delta={}, error={}", dossierItemId, delta, e.getMessage());
        }
    }

    /**
     * 重新排序文档
     * @param documentIds 按顺序排列的文档ID列表
     */
    @Transactional
    public void reorderDocuments(List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < documentIds.size(); i++) {
            Long docId = documentIds.get(i);
            Document doc = documentRepository.getById(docId);
            if (doc != null) {
                doc.setDisplayOrder(i + 1);
                documentRepository.updateById(doc);
            }
        }
        
        log.info("文档排序更新成功: count={}", documentIds.size());
    }

    /**
     * 为已存在的文档生成缩略图
     * @param documentId 文档ID
     * @return 缩略图URL，如果不支持或生成失败返回null
     */
    @Transactional
    public String generateThumbnailForDocument(Long documentId) {
        Document document = documentRepository.getByIdOrThrow(documentId, "文档不存在");
        
        // 如果已有缩略图，直接返回
        if (document.getThumbnailUrl() != null && !document.getThumbnailUrl().isEmpty()) {
            return document.getThumbnailUrl();
        }
        
        // 检查是否支持生成缩略图
        if (!thumbnailService.supportsThumbnail(document.getFileName())) {
            log.debug("文件类型不支持缩略图: {}", document.getFileName());
            return null;
        }
        
        // 从MinIO获取文件并生成缩略图
        String thumbnailUrl = thumbnailService.generateThumbnailFromUrl(
                document.getFilePath(), 
                document.getFileName()
        );
        
        if (thumbnailUrl != null) {
            document.setThumbnailUrl(thumbnailUrl);
            documentRepository.updateById(document);
            log.info("文档缩略图生成成功: docId={}, thumbnailUrl={}", documentId, thumbnailUrl);
        }
        
        return thumbnailUrl;
    }

    /**
     * 获取文档缩略图URL
     * 如果缩略图不存在，尝试生成
     * @param documentId 文档ID
     * @return 缩略图URL
     */
    public String getThumbnailUrl(Long documentId) {
        Document document = documentRepository.getByIdOrThrow(documentId, "文档不存在");
        
        // 如果已有缩略图，转换为浏览器可访问的 URL 后返回
        if (document.getThumbnailUrl() != null && !document.getThumbnailUrl().isEmpty()) {
            return minioService.getBrowserAccessibleUrl(document.getThumbnailUrl());
        }
        
        // 尝试生成缩略图
        String thumbnailUrl = generateThumbnailForDocument(documentId);
        if (thumbnailUrl != null) {
            return minioService.getBrowserAccessibleUrl(thumbnailUrl);
        }
        return null;
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
     * 从 OnlyOffice 保存编辑后的文档
     * OnlyOffice 在用户完成编辑后会调用回调接口，提供编辑后文档的下载URL
     *
     * @param documentId  文档ID
     * @param downloadUrl OnlyOffice 提供的编辑后文档下载URL
     */
    @Transactional
    public void saveFromOnlyOffice(Long documentId, String downloadUrl) {
        Document document = documentRepository.getById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在: " + documentId);
        }
        
        try {
            // 1. 从 OnlyOffice 下载编辑后的文档
            // OnlyOffice 提供的 downloadUrl 可能包含 localhost，需要替换为 Docker 内部地址
            String accessibleUrl = normalizeOnlyOfficeDownloadUrl(downloadUrl);
            log.info("OnlyOffice 下载 URL 规范化: {} -> {}", downloadUrl, accessibleUrl);
            
            // ⚠️ 内存泄露修复：使用 try-with-resources 确保 InputStream 正确关闭
            // 使用 URI 转换为 URL（避免使用已废弃的 URL 构造函数）
            java.net.URL url = new java.net.URI(accessibleUrl).toURL();
            byte[] fileContent;
            try (java.io.InputStream inputStream = url.openStream();
                 java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            // 2. 计算文件大小（需要先读取到 ByteArrayOutputStream）
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
                fileContent = baos.toByteArray();
            }
            long newFileSize = fileContent.length;
            
            // 3. 构建新的存储路径（保持原来的路径结构）
            String storagePath = buildStoragePath(document.getMatterId(), document.getFolderPath());
            String newFileName = document.getFileName();
            String objectName = storagePath + System.currentTimeMillis() + "_" + newFileName;
            
            // 4. 上传新文件到 MinIO
            java.io.ByteArrayInputStream uploadStream = new java.io.ByteArrayInputStream(fileContent);
            String newFileUrl = minioService.uploadFile(uploadStream, objectName, document.getMimeType());
            
            // 5. 更新文档记录（不增加版本号，保持 documentKey 不变）
            // 注意：OnlyOffice 使用 documentKey 识别编辑会话
            // 如果版本号改变，documentKey 也会变，导致下次打开时提示"版本更新，无法编辑"
            // 只有用户手动"上传新版本"时才增加版本号
            document.setFilePath(newFileUrl);
            document.setFileSize(newFileSize);
            // 不改变版本号：document.setVersion(newVersion);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.updateById(document);
            
            // 6. 保存编辑记录（作为历史记录，但不作为新版本）
            saveEditHistory(document, "OnlyOffice 在线编辑保存");
            
            log.info("OnlyOffice 文档保存成功: id={}, version={} (版本号未变), newPath={}", 
                    documentId, document.getVersion(), newFileUrl);
            
        } catch (Exception e) {
            log.error("从 OnlyOffice 保存文档失败: documentId={}, url={}", documentId, downloadUrl, e);
            throw new BusinessException("保存文档失败: " + e.getMessage());
        }
    }

    /**
     * 规范化 OnlyOffice 提供的下载 URL
     * 将 localhost、127.0.0.1 等地址替换为 Docker 内部可访问的地址
     */
    private String normalizeOnlyOfficeDownloadUrl(String downloadUrl) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            return downloadUrl;
        }
        
        // OnlyOffice 提供的 URL 通常是它自己服务器上的临时文件
        // 如果包含 localhost 或 127.0.0.1，需要替换为 Docker 内部服务名
        String normalizedUrl = downloadUrl;
        
        // 替换 localhost 和 127.0.0.1 为 onlyoffice（Docker 服务名）
        // 注意：OnlyOffice 容器在 Docker 网络中可以通过服务名访问
        normalizedUrl = normalizedUrl.replace("http://localhost/", "http://onlyoffice/")
                .replace("http://127.0.0.1/", "http://onlyoffice/")
                .replace("http://localhost:80/", "http://onlyoffice/")
                .replace("http://127.0.0.1:80/", "http://onlyoffice/")
                .replace("http://localhost:8088/", "http://onlyoffice/")
                .replace("http://127.0.0.1:8088/", "http://onlyoffice/");
        
        // 如果 URL 包含 host.docker.internal，也替换为 onlyoffice
        normalizedUrl = normalizedUrl.replace("http://host.docker.internal/", "http://onlyoffice/")
                .replace("http://host.docker.internal:80/", "http://onlyoffice/")
                .replace("http://host.docker.internal:8088/", "http://onlyoffice/");
        
        return normalizedUrl;
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
        dto.setDossierItemId(doc.getDossierItemId());
        dto.setFolderPath(doc.getFolderPath());
        dto.setDisplayOrder(doc.getDisplayOrder());
        // 转换缩略图 URL 为浏览器可访问的 URL
        String thumbnailUrl = doc.getThumbnailUrl();
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            thumbnailUrl = minioService.getBrowserAccessibleUrl(thumbnailUrl);
        }
        dto.setThumbnailUrl(thumbnailUrl);
        return dto;
    }
}
