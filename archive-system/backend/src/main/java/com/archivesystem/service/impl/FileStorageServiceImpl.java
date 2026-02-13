package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ConfigService;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioService minioService;
    private final DigitalFileMapper digitalFileMapper;
    private final ConfigService configService;

    // 配置键常量
    private static final String CONFIG_MAX_FILE_SIZE = "system.upload.max.size";
    private static final String CONFIG_ALLOWED_TYPES = "system.upload.allowed.types";
    private static final long DEFAULT_MAX_FILE_SIZE = 104857600L; // 100MB
    private static final String DEFAULT_ALLOWED_TYPES = "pdf,doc,docx,xls,xlsx,ppt,pptx,jpg,jpeg,png,gif,zip,rar,txt,ofd";

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final Set<String> LONG_TERM_FORMATS = Set.of("pdf", "ofd", "tif", "tiff");

    @Override
    @Transactional
    public DigitalFile upload(MultipartFile file, Long archiveId, String fileCategory) {
        // 校验文件
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension = getFileExtension(originalName);
        String mimeType = file.getContentType();

        // 生成存储路径
        String storagePath = generateStoragePath(archiveId, extension);

        try {
            // 计算文件哈希
            byte[] content = file.getBytes();
            String hashValue = calculateHash(content);

            // 上传到MinIO
            minioService.upload(storagePath, file.getInputStream(), file.getSize(), mimeType);

            // 保存文件记录
            DigitalFile digitalFile = DigitalFile.builder()
                    .archiveId(archiveId)
                    .fileName(generateFileName(originalName))
                    .originalName(originalName)
                    .fileExtension(extension)
                    .mimeType(mimeType)
                    .fileSize(file.getSize())
                    .storagePath(storagePath)
                    .storageBucket(minioService.getBucketName())
                    .formatName(getFormatName(extension))
                    .isLongTermFormat(LONG_TERM_FORMATS.contains(extension.toLowerCase()))
                    .hashAlgorithm("SHA-256")
                    .hashValue(hashValue)
                    .fileCategory(fileCategory != null ? fileCategory : DigitalFile.CATEGORY_MAIN)
                    .uploadAt(LocalDateTime.now())
                    .uploadBy(SecurityUtils.getCurrentUserId())
                    .build();

            digitalFileMapper.insert(digitalFile);
            log.info("文件上传成功: id={}, path={}", digitalFile.getId(), storagePath);

            return digitalFile;

        } catch (Exception e) {
            log.error("文件上传失败: {}", originalName, e);
            throw new BusinessException("4001", "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public DigitalFile downloadAndStore(Long archiveId, String downloadUrl, String fileName, 
                                        String fileCategory, int sortOrder) {
        log.info("下载文件: archiveId={}, url={}", archiveId, downloadUrl);

        try {
            URL url = new URL(downloadUrl);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            String mimeType = conn.getContentType();
            long fileSize = conn.getContentLengthLong();
            String extension = getFileExtension(fileName);

            // 生成存储路径
            String storagePath = generateStoragePath(archiveId, extension);

            try (InputStream inputStream = conn.getInputStream()) {
                // 读取内容用于计算哈希
                byte[] content = inputStream.readAllBytes();
                String hashValue = calculateHash(content);

                // 上传到MinIO
                minioService.upload(storagePath, new java.io.ByteArrayInputStream(content), content.length, mimeType);

                // 保存文件记录
                DigitalFile digitalFile = DigitalFile.builder()
                        .archiveId(archiveId)
                        .fileName(generateFileName(fileName))
                        .originalName(fileName)
                        .fileExtension(extension)
                        .mimeType(mimeType)
                        .fileSize((long) content.length)
                        .storagePath(storagePath)
                        .storageBucket(minioService.getBucketName())
                        .formatName(getFormatName(extension))
                        .isLongTermFormat(LONG_TERM_FORMATS.contains(extension.toLowerCase()))
                        .hashAlgorithm("SHA-256")
                        .hashValue(hashValue)
                        .fileCategory(fileCategory != null ? fileCategory : DigitalFile.CATEGORY_MAIN)
                        .sortOrder(sortOrder)
                        .sourceUrl(downloadUrl)
                        .uploadAt(LocalDateTime.now())
                        .build();

                digitalFileMapper.insert(digitalFile);
                log.info("文件下载存储成功: id={}, path={}", digitalFile.getId(), storagePath);

                return digitalFile;
            }

        } catch (Exception e) {
            log.error("文件下载失败: {}", downloadUrl, e);
            return null;
        }
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }
        return minioService.getPresignedUrl(file.getStoragePath(), 3600);
    }

    @Override
    public String getPreviewUrl(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }

        // 如果有预览文件，返回预览文件URL
        if (StringUtils.hasText(file.getPreviewPath())) {
            return minioService.getPresignedUrl(file.getPreviewPath(), 3600);
        }

        // 对于图片和PDF，直接返回原文件URL
        String ext = file.getFileExtension().toLowerCase();
        if (IMAGE_EXTENSIONS.contains(ext) || "pdf".equals(ext)) {
            return minioService.getPresignedUrl(file.getStoragePath(), 3600);
        }

        return null;
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            return;
        }

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

    private long getMaxFileSize() {
        Integer maxSize = configService.getIntValue(CONFIG_MAX_FILE_SIZE, null);
        return maxSize != null ? maxSize.longValue() : DEFAULT_MAX_FILE_SIZE;
    }

    private String getAllowedTypes() {
        return configService.getValue(CONFIG_ALLOWED_TYPES, DEFAULT_ALLOWED_TYPES);
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
        String baseName = originalName;
        if (StringUtils.hasText(extension)) {
            baseName = originalName.substring(0, originalName.length() - extension.length() - 1);
        }
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }
        return baseName + "_" + uuid + (StringUtils.hasText(extension) ? "." + extension : "");
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
}
