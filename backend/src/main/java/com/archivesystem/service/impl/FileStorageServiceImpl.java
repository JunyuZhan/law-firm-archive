package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.ForbiddenException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.dto.file.FilePreviewInfo;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.security.OutboundUrlValidator;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.DocumentConversionService;
import com.archivesystem.service.FileStorageService;
import com.archivesystem.service.MinioService;
import com.archivesystem.util.FileTypeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    private static final String FILE_UPLOAD_FAILURE_PUBLIC_MESSAGE = "文件上传失败，请稍后重试或联系系统管理员";
    private static final String FILE_STORE_FAILURE_PUBLIC_MESSAGE = "文件存储失败，请稍后重试或联系系统管理员";

    private final MinioService minioService;
    private final DigitalFileMapper digitalFileMapper;
    private final ArchiveMapper archiveMapper;
    private final BorrowApplicationMapper borrowApplicationMapper;
    private final ConfigService configService;
    private final DocumentConversionService documentConversionService;
    private final OutboundUrlValidator outboundUrlValidator;

    // 配置键常量
    private static final String CONFIG_MAX_FILE_SIZE = "system.upload.max.size";
    private static final String CONFIG_ALLOWED_TYPES = "system.upload.allowed.types";
    private static final long DEFAULT_MAX_FILE_SIZE = 104857600L; // 100MB
    private static final String DEFAULT_ALLOWED_TYPES = "pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,gif,zip,rar,txt,ofd";

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");

    @Override
    @Transactional
    public DigitalFile upload(MultipartFile file, Long archiveId, String fileCategory) {
        return upload(file, archiveId, fileCategory,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    @Override
    @Transactional
    public DigitalFile upload(MultipartFile file, Long archiveId, String fileCategory,
                              Integer volumeNo, String sectionType, String documentNo,
                              Integer pageStart, Integer pageEnd, String versionLabel,
                              String fileSourceType, String scanBatchNo, String scanOperator,
                              LocalDateTime scanTime, String scanCheckStatus,
                              String scanCheckBy, LocalDateTime scanCheckTime) {
        // 校验文件
        validateFile(file);

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = getFileExtension(originalName);
        String mimeType = file.getContentType();

        // 生成存储路径
        String storagePath = generateStoragePath(archiveId, extension);

        try {
            // 读取文件内容
            byte[] content = file.getBytes();
            String hashValue = calculateHash(content);

            // 上传原文件到MinIO
            minioService.upload(storagePath, file.getInputStream(), file.getSize(), mimeType);

            // 构建文件记录
            DigitalFile.DigitalFileBuilder fileBuilder = DigitalFile.builder()
                    .archiveId(archiveId)
                    .fileName(generateFileName(originalName))
                    .originalName(originalName)
                    .fileExtension(extension)
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .storageBucket(minioService.getBucketName())
                    .formatName(getFormatName(extension))
                    .hashAlgorithm("SHA-256")
                    .hashValue(hashValue)
                    .fileCategory(fileCategory != null ? fileCategory : DigitalFile.CATEGORY_MAIN)
                    .volumeNo(volumeNo)
                    .sectionType(sectionType)
                    .documentNo(documentNo)
                    .pageStart(pageStart)
                    .pageEnd(pageEnd)
                    .versionLabel(versionLabel)
                    .fileSourceType(StringUtils.hasText(fileSourceType) ? fileSourceType : DigitalFile.SOURCE_IMPORTED)
                    .scanBatchNo(scanBatchNo)
                    .scanOperator(scanOperator)
                    .scanTime(scanTime)
                    .scanCheckStatus(scanCheckStatus)
                    .scanCheckBy(scanCheckBy)
                    .scanCheckTime(scanCheckTime)
                    .uploadAt(LocalDateTime.now())
                    .uploadBy(SecurityUtils.getCurrentUserId());

            // 检查是否需要转换为PDF（Office文档 -> PDF）
            String convertedPath = null;
            if (documentConversionService.needsConversion(extension)) {
                log.info("检测到Office文档，开始转换为PDF: {}", originalName);
                byte[] pdfContent = documentConversionService.convertToPdf(content, extension);
                
                if (pdfContent != null) {
                    // 生成PDF存储路径
                    convertedPath = generateStoragePath(archiveId, "pdf");
                    minioService.upload(convertedPath, 
                            new java.io.ByteArrayInputStream(pdfContent), 
                            pdfContent.length, 
                            "application/pdf");
                    
                    fileBuilder.convertedPath(convertedPath)
                              .isLongTermFormat(true);  // 转换后视为长期保存格式
                    log.info("Office文档转换PDF成功: {} -> {}", originalName, convertedPath);
                } else {
                    // 转换失败，保留原文件但标记为非长期保存格式
                    fileBuilder.isLongTermFormat(false);
                    log.warn("Office文档转换PDF失败，保留原文件: {}", originalName);
                }
            } else {
                // 非Office格式，检查是否为长期保存格式
                fileBuilder.isLongTermFormat(documentConversionService.isLongTermFormat(extension));
            }

            DigitalFile digitalFile = fileBuilder.build();
            digitalFileMapper.insert(digitalFile);
            refreshArchiveFileStats(archiveId);
            log.info("文件上传成功: id={}, path={}, convertedPath={}", 
                    digitalFile.getId(), storagePath, convertedPath);

            return digitalFile;

        } catch (Exception e) {
            log.error("文件上传失败: {}", originalName, e);
            throw new BusinessException("4001", FILE_UPLOAD_FAILURE_PUBLIC_MESSAGE);
        }
    }

    // 下载重试次数
    private static final int DOWNLOAD_MAX_RETRIES = 3;
    // 下载重试间隔（毫秒）
    private static final long DOWNLOAD_RETRY_DELAY = 2000;

    @Override
    @Transactional
    public DigitalFile downloadAndStore(Long archiveId, String downloadUrl, String fileName, 
                                        String fileCategory, int sortOrder) {
        log.info("下载文件: archiveId={}, url={}", archiveId, downloadUrl);
        outboundUrlValidator.validate(downloadUrl, "文件下载地址");

        String extension = getFileExtension(fileName);
        
        // 检查文件类型是否允许
        if (!isAllowedType(extension)) {
            log.warn("不支持的文件类型，拒绝下载: extension={}, fileName={}", extension, fileName);
            return null;
        }
        
        // 带重试的下载
        byte[] content = downloadWithRetry(downloadUrl, fileName);
        if (content == null) {
            log.error("文件下载失败，已重试{}次: {}", DOWNLOAD_MAX_RETRIES, downloadUrl);
            return null;
        }

        // 验证下载内容的文件类型（魔数验证）
        if (!validateDownloadedContent(content, extension, fileName)) {
            log.warn("下载内容安全验证失败: fileName={}", fileName);
            return null;
        }

        String mimeType = getMimeType(extension);
        
        // 生成存储路径
        String storagePath = generateStoragePath(archiveId, extension);

        try {
            String hashValue = calculateHash(content);

            // 上传原文件到MinIO
            minioService.upload(storagePath, new java.io.ByteArrayInputStream(content), content.length, mimeType);

            // 构建文件记录
            DigitalFile.DigitalFileBuilder fileBuilder = DigitalFile.builder()
                    .archiveId(archiveId)
                    .fileName(generateFileName(fileName))
                    .originalName(fileName)
                    .fileExtension(extension)
                    .mimeType(mimeType)
                    .fileSize((long) content.length)
                    .storagePath(storagePath)
                    .storageBucket(minioService.getBucketName())
                    .formatName(getFormatName(extension))
                    .hashAlgorithm("SHA-256")
                    .hashValue(hashValue)
                    .fileCategory(fileCategory != null ? fileCategory : DigitalFile.CATEGORY_MAIN)
                    .sortOrder(sortOrder)
                    .fileSourceType(DigitalFile.SOURCE_IMPORTED)
                    .sourceUrl(downloadUrl)
                    .uploadAt(LocalDateTime.now());

            // 检查是否需要转换为PDF（Office文档 -> PDF）
            String convertedPath = null;
            if (documentConversionService.needsConversion(extension)) {
                log.info("检测到Office文档，开始转换为PDF: {}", fileName);
                byte[] pdfContent = documentConversionService.convertToPdf(content, extension);
                
                if (pdfContent != null) {
                    // 生成PDF存储路径
                    convertedPath = generateStoragePath(archiveId, "pdf");
                    minioService.upload(convertedPath, 
                            new java.io.ByteArrayInputStream(pdfContent), 
                            pdfContent.length, 
                            "application/pdf");
                    
                    fileBuilder.convertedPath(convertedPath)
                              .isLongTermFormat(true);  // 转换后视为长期保存格式
                    log.info("Office文档转换PDF成功: {} -> {}", fileName, convertedPath);
                } else {
                    // 转换失败，保留原文件但标记为非长期保存格式
                    fileBuilder.isLongTermFormat(false);
                    log.warn("Office文档转换PDF失败，保留原文件: {}", fileName);
                }
            } else {
                // 非Office格式，检查是否为长期保存格式
                fileBuilder.isLongTermFormat(documentConversionService.isLongTermFormat(extension));
            }

            DigitalFile digitalFile = fileBuilder.build();
            digitalFileMapper.insert(digitalFile);
            refreshArchiveFileStats(archiveId);
            log.info("文件下载存储成功: id={}, path={}, convertedPath={}", 
                    digitalFile.getId(), storagePath, convertedPath);

            return digitalFile;

        } catch (Exception e) {
            log.error("文件存储失败: {}", downloadUrl, e);
            // 清理已上传到MinIO的文件
            cleanupMinioFile(storagePath);
            throw new BusinessException(FILE_STORE_FAILURE_PUBLIC_MESSAGE);
        }
    }

    /**
     * 带重试的文件下载.
     */
    private byte[] downloadWithRetry(String downloadUrl, String fileName) {
        long maxFileSize = getMaxFileSize();
        
        for (int retry = 0; retry < DOWNLOAD_MAX_RETRIES; retry++) {
            try {
                URL url = URI.create(downloadUrl).toURL();
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                
                // 检查文件大小
                long contentLength = conn.getContentLengthLong();
                if (contentLength > 0 && contentLength > maxFileSize) {
                    log.error("文件大小超出限制: {} > {} bytes, file={}", contentLength, maxFileSize, fileName);
                    return null;
                }
                
                try (InputStream inputStream = conn.getInputStream()) {
                    // 流式读取，限制最大大小
                    java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                    byte[] chunk = new byte[8192];
                    int bytesRead;
                    long totalRead = 0;
                    
                    while ((bytesRead = inputStream.read(chunk)) != -1) {
                        totalRead += bytesRead;
                        if (totalRead > maxFileSize) {
                            log.error("文件下载超出大小限制: {} bytes, file={}", totalRead, fileName);
                            return null;
                        }
                        buffer.write(chunk, 0, bytesRead);
                    }
                    
                    log.info("文件下载成功: file={}, size={} bytes", fileName, totalRead);
                    return buffer.toByteArray();
                }
                
            } catch (Exception e) {
                log.warn("文件下载失败，第{}次重试: file={}, error={}", retry + 1, fileName, e.getMessage());
                if (retry < DOWNLOAD_MAX_RETRIES - 1) {
                    try {
                        Thread.sleep(DOWNLOAD_RETRY_DELAY * (retry + 1)); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 清理MinIO上的孤儿文件.
     */
    private void cleanupMinioFile(String storagePath) {
        if (storagePath != null) {
            try {
                minioService.delete(storagePath);
                log.info("已清理孤儿文件: {}", storagePath);
            } catch (Exception e) {
                log.warn("清理孤儿文件失败: {}", storagePath, e);
            }
        }
    }

    /**
     * 根据扩展名获取MIME类型.
     */
    private String getMimeType(String extension) {
        if (extension == null) return "application/octet-stream";
        return switch (extension.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            default -> "application/octet-stream";
        };
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }
        assertFileAccessAuthorized(file, true);
        return minioService.getPresignedUrl(file.getStoragePath(), 3600);
    }

    @Override
    public String getPreviewUrl(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }
        assertFileAccessAuthorized(file, false);

        // 优先返回转换后的PDF（对于Office文档）
        if (StringUtils.hasText(file.getConvertedPath())) {
            log.debug("返回转换后的PDF预览URL: fileId={}, convertedPath={}", fileId, file.getConvertedPath());
            return minioService.getPresignedUrl(file.getConvertedPath(), 3600);
        }

        // 如果有预览文件，返回预览文件URL
        if (StringUtils.hasText(file.getPreviewPath())) {
            return minioService.getPresignedUrl(file.getPreviewPath(), 3600);
        }

        // 对于图片和PDF，直接返回原文件URL
        String ext = file.getFileExtension();
        if (ext == null || ext.isEmpty()) {
            log.warn("文件扩展名为空，无法预览: fileId={}", fileId);
            return null;
        }
        ext = ext.toLowerCase();
        if (IMAGE_EXTENSIONS.contains(ext) || "pdf".equals(ext) || "ofd".equals(ext)) {
            return minioService.getPresignedUrl(file.getStoragePath(), 3600);
        }

        // 其他格式不支持预览
        log.debug("文件不支持预览: fileId={}, extension={}", fileId, ext);
        return null;
    }

    @Override
    public void assertPreviewAccess(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }
        assertFileAccessAuthorized(file, false);
    }

    @Override
    public FilePreviewInfo getPreviewInfo(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }
        assertFileAccessAuthorized(file, false);

        String ext = file.getFileExtension();
        if (ext == null) {
            ext = "";
        } else {
            ext = ext.toLowerCase();
        }
        
        String previewUrl = null;
        String previewType = "unsupported";
        boolean isConverted = false;

        // 优先使用转换后的PDF
        if (StringUtils.hasText(file.getConvertedPath())) {
            previewUrl = minioService.getPresignedUrl(file.getConvertedPath(), 3600);
            previewType = "pdf";
            isConverted = true;
            log.debug("返回转换后的PDF预览: fileId={}, convertedPath={}", fileId, file.getConvertedPath());
        }
        // 有预览文件
        else if (StringUtils.hasText(file.getPreviewPath())) {
            previewUrl = minioService.getPresignedUrl(file.getPreviewPath(), 3600);
            previewType = "pdf";
        }
        // 原生PDF或OFD
        else if ("pdf".equals(ext) || "ofd".equals(ext)) {
            previewUrl = minioService.getPresignedUrl(file.getStoragePath(), 3600);
            previewType = "pdf";
        }
        // 图片
        else if (IMAGE_EXTENSIONS.contains(ext) || "tif".equals(ext) || "tiff".equals(ext)) {
            previewUrl = minioService.getPresignedUrl(file.getStoragePath(), 3600);
            previewType = "image";
        }
        // 视频
        else if (Set.of("mp4", "webm", "ogg", "mov").contains(ext)) {
            previewUrl = minioService.getPresignedUrl(file.getStoragePath(), 3600);
            previewType = "video";
        }
        // 音频
        else if (Set.of("mp3", "wav", "ogg", "flac", "m4a").contains(ext)) {
            previewUrl = minioService.getPresignedUrl(file.getStoragePath(), 3600);
            previewType = "audio";
        }

        return FilePreviewInfo.builder()
                .url(previewUrl)
                .previewType(previewType)
                .isConverted(isConverted)
                .build();
    }

    @Override
    public String getBorrowPreviewUrl(Long archiveId, Long fileId, int expirySeconds) {
        DigitalFile file = getArchiveFile(archiveId, fileId);

        if (StringUtils.hasText(file.getConvertedPath())) {
            return minioService.getPresignedUrl(file.getConvertedPath(), expirySeconds);
        }
        if (StringUtils.hasText(file.getPreviewPath())) {
            return minioService.getPresignedUrl(file.getPreviewPath(), expirySeconds);
        }

        String ext = file.getFileExtension();
        if (!StringUtils.hasText(ext)) {
            return null;
        }
        ext = ext.toLowerCase();
        if (IMAGE_EXTENSIONS.contains(ext) || "pdf".equals(ext) || "ofd".equals(ext)
                || "tif".equals(ext) || "tiff".equals(ext)) {
            return minioService.getPresignedUrl(file.getStoragePath(), expirySeconds);
        }
        return null;
    }

    @Override
    public String getBorrowDownloadUrl(Long archiveId, Long fileId, int expirySeconds) {
        DigitalFile file = getArchiveFile(archiveId, fileId);
        return minioService.getPresignedUrl(file.getStoragePath(), expirySeconds);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            return;
        }
        assertFileDeletionAllowed(file);

        try {
            // 删除MinIO中的文件
            minioService.delete(file.getStoragePath());
            if (StringUtils.hasText(file.getPreviewPath())) {
                minioService.delete(file.getPreviewPath());
            }
            if (StringUtils.hasText(file.getThumbnailPath())) {
                minioService.delete(file.getThumbnailPath());
            }

            // 删除数据库记录
            digitalFileMapper.deleteById(fileId);
            refreshArchiveFileStats(file.getArchiveId());
            log.info("文件删除成功: id={}", fileId);

        } catch (Exception e) {
            log.error("文件删除失败: id={}", fileId, e);
            throw new BusinessException("文件删除失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("4001", "请选择要上传的文件");
        }

        long maxFileSize = getMaxFileSize();
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("4004", "文件大小超出限制（最大" + (maxFileSize / 1024 / 1024) + "MB）");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!isAllowedType(extension)) {
            throw new BusinessException("4003", "不支持的文件类型: " + extension);
        }
        
        // 魔数验证：检查文件真实类型是否与扩展名匹配
        FileTypeValidator.ValidationResult validationResult = FileTypeValidator.validate(file, extension);
        if (!validationResult.isValid()) {
            log.warn("文件魔数验证失败: filename={}, extension={}, reason={}", 
                    file.getOriginalFilename(), extension, validationResult.getMessage());
            throw new BusinessException("4005", "文件安全校验失败: " + validationResult.getMessage());
        }
        
        // MIME类型验证
        String mimeType = file.getContentType();
        if (!FileTypeValidator.validateMimeType(extension, mimeType)) {
            log.warn("MIME类型验证失败: filename={}, extension={}, mimeType={}", 
                    file.getOriginalFilename(), extension, mimeType);
            throw new BusinessException("4006", "文件MIME类型与扩展名不匹配");
        }
        
        log.debug("文件验证通过: filename={}, extension={}, size={}", 
                file.getOriginalFilename(), extension, file.getSize());
    }

    private void refreshArchiveFileStats(Long archiveId) {
        if (archiveId == null) {
            return;
        }
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            return;
        }
        var fileStats = digitalFileMapper.countByArchiveId(archiveId);
        Number countValue = fileStats != null ? (Number) fileStats.get("count") : null;
        Number totalSizeValue = fileStats != null ? (Number) fileStats.get("total_size") : null;
        int fileCount = countValue != null ? countValue.intValue() : 0;
        long totalSize = totalSizeValue != null ? totalSizeValue.longValue() : 0L;

        archive.setFileCount(fileCount);
        archive.setTotalFileSize(totalSize);
        archive.setHasElectronic(fileCount > 0);
        archiveMapper.updateById(archive);
    }

    private long getMaxFileSize() {
        Integer maxSize = configService.getIntValue(CONFIG_MAX_FILE_SIZE, null);
        return maxSize != null ? maxSize.longValue() : DEFAULT_MAX_FILE_SIZE;
    }

    private String getAllowedTypes() {
        String allowedTypes = configService.getValue(CONFIG_ALLOWED_TYPES, DEFAULT_ALLOWED_TYPES);
        return (allowedTypes == null || allowedTypes.isBlank()) ? DEFAULT_ALLOWED_TYPES : allowedTypes;
    }

    private boolean isAllowedType(String extension) {
        if (!StringUtils.hasText(extension)) {
            return false;
        }
        String[] allowed = getAllowedTypes().split(",");
        for (String type : allowed) {
            if (type.trim().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证下载内容的安全性（魔数验证）.
     */
    private boolean validateDownloadedContent(byte[] content, String extension, String fileName) {
        if (content == null || content.length == 0) {
            return false;
        }
        
        // 使用字节数组直接进行魔数验证
        FileTypeValidator.ValidationResult result = FileTypeValidator.validate(content, extension);
        if (!result.isValid()) {
            log.warn("下载内容魔数验证失败: fileName={}, reason={}", fileName, result.getMessage());
            return false;
        }
        return true;
    }

    private String getFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private String generateStoragePath(Long archiveId, String extension) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("archives/%s/%d/%s.%s", date, archiveId != null ? archiveId : 0, uuid, extension);
    }

    private String generateFileName(String originalName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = getFileExtension(originalName);
        String baseName = sanitizeFileName(originalName);
        if (StringUtils.hasText(extension)) {
            int extLength = extension.length() + 1;
            if (baseName.length() > extLength) {
                baseName = baseName.substring(0, baseName.length() - extLength);
            }
        }
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }
        return baseName + "_" + uuid + (StringUtils.hasText(extension) ? "." + extension : "");
    }
    
    /**
     * 清理文件名，防止路径穿越和XSS攻击
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unnamed";
        }
        
        // 移除路径穿越字符和危险字符
        String sanitized = fileName
                .replace("..", "")           // 路径穿越
                .replace("/", "_")           // Unix路径分隔符
                .replace("\\", "_")          // Windows路径分隔符
                .replace("\0", "")           // 空字节
                .replace("<", "_")           // HTML标签（XSS）
                .replace(">", "_")           // HTML标签（XSS）
                .replace("\"", "_")          // 引号
                .replace("'", "_")           // 单引号
                .replace("&", "_")           // HTML实体
                .replace("|", "_")           // 管道符
                .replace(":", "_")           // 冒号（Windows路径）
                .replace("*", "_")           // 通配符
                .replace("?", "_");          // 通配符
        
        // 限制长度
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
        // 确保不为空
        if (sanitized.trim().isEmpty()) {
            return "unnamed";
        }
        
        return sanitized.trim();
    }

    private String calculateHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            return null;
        }
    }

    private String getFormatName(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "未知";
        }
        return switch (extension.toLowerCase()) {
            case "pdf" -> "PDF文档";
            case "doc", "docx" -> "Word文档";
            case "xls", "xlsx" -> "Excel表格";
            case "ppt", "pptx" -> "PowerPoint演示文稿";
            case "jpg", "jpeg" -> "JPEG图片";
            case "png" -> "PNG图片";
            case "gif" -> "GIF图片";
            case "zip" -> "ZIP压缩包";
            case "rar" -> "RAR压缩包";
            case "txt" -> "文本文件";
            default -> extension.toUpperCase() + "文件";
        };
    }

    private void assertFileAccessAuthorized(DigitalFile file, boolean download) {
        if (SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_REVIEWER", "ARCHIVE_MANAGER")) {
            return;
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null || file.getArchiveId() == null) {
            throw new ForbiddenException("无权访问该文件");
        }

        Archive archive = archiveMapper.selectById(file.getArchiveId());
        if (archive != null && isArchiveReadableByCurrentUser(archive)) {
            return;
        }

        String currentDepartment = SecurityUtils.getCurrentDepartment();
        var applications = borrowApplicationMapper.selectByArchiveId(file.getArchiveId());
        boolean allowed = applications.stream().anyMatch(application ->
                currentUserId.equals(application.getApplicantId())
                        && isBorrowAccessAllowed(application, archive, currentDepartment, download));
        if (!allowed) {
            throw new ForbiddenException("无权访问该文件");
        }
    }

    private boolean isBorrowAccessAllowed(BorrowApplication application,
                                          Archive archive,
                                          String currentDepartment,
                                          boolean download) {
        if (application == null) {
            return false;
        }
        boolean statusAllowed = BorrowApplication.STATUS_APPROVED.equals(application.getStatus())
                || BorrowApplication.STATUS_BORROWED.equals(application.getStatus());
        if (!statusAllowed) {
            return false;
        }
        if (StringUtils.hasText(application.getApplicantDept())
                && StringUtils.hasText(currentDepartment)
                && !application.getApplicantDept().equals(currentDepartment)) {
            return false;
        }
        String borrowType = application.getBorrowType();
        if (download) {
            if (!BorrowApplication.TYPE_DOWNLOAD.equals(borrowType)) {
                return false;
            }
            return archive == null
                    || (!Archive.SECURITY_CONFIDENTIAL.equals(archive.getSecurityLevel())
                    && !Archive.SECURITY_SECRET.equals(archive.getSecurityLevel()));
        }
        return BorrowApplication.TYPE_ONLINE.equals(borrowType)
                || BorrowApplication.TYPE_DOWNLOAD.equals(borrowType);
    }

    private void assertFileDeletionAllowed(DigitalFile file) {
        if (file.getArchiveId() == null) {
            return;
        }

        Archive archive = archiveMapper.selectById(file.getArchiveId());
        if (archive != null && Archive.STATUS_STORED.equals(archive.getStatus())) {
            throw new BusinessException("已归档档案的原始文件不可删除");
        }
    }

    private boolean isArchiveReadableByCurrentUser(Archive archive) {
        if (!SecurityUtils.isAuthenticated()) {
            return false;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentRealName = SecurityUtils.getCurrentRealName();
        boolean ownArchive = (currentUserId != null
                && (currentUserId.equals(archive.getCreatedBy())
                || currentUserId.equals(archive.getReceivedBy())))
                || (StringUtils.hasText(currentRealName) && currentRealName.equals(archive.getLawyerName()));
        boolean securityAllowed = Archive.SECURITY_PUBLIC.equals(archive.getSecurityLevel())
                || Archive.SECURITY_INTERNAL.equals(archive.getSecurityLevel())
                || !StringUtils.hasText(archive.getSecurityLevel());
        return ownArchive && securityAllowed;
    }

    private DigitalFile getArchiveFile(Long archiveId, Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null || !archiveId.equals(file.getArchiveId())) {
            throw NotFoundException.of("文件", fileId);
        }
        return file;
    }
}
