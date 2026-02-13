package com.archivesystem.service.impl;

import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.DigitalFile;
import com.archivesystem.repository.DigitalFileMapper;
import com.archivesystem.service.AccessLogService;
import com.archivesystem.service.MinioService;
import com.archivesystem.service.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件预览服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {

    private final MinioService minioService;
    private final DigitalFileMapper digitalFileMapper;
    private final AccessLogService accessLogService;

    // 支持直接预览的文件类型
    private static final Set<String> DIRECT_PREVIEWABLE = Set.of(
            "pdf", "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    );

    // 支持生成缩略图的图片类型
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    // 缩略图最大尺寸
    private static final int THUMBNAIL_MAX_WIDTH = 300;
    private static final int THUMBNAIL_MAX_HEIGHT = 300;

    @Override
    public String generateThumbnail(Long fileId) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }

        // 如果已有缩略图，直接返回
        if (StringUtils.hasText(file.getThumbnailPath())) {
            return file.getThumbnailPath();
        }

        String ext = file.getFileExtension();
        if (!IMAGE_EXTENSIONS.contains(ext != null ? ext.toLowerCase() : "")) {
            log.debug("不支持生成缩略图的文件类型: {}", ext);
            return null;
        }

        try {
            // 下载原图
            InputStream inputStream = minioService.getFile(file.getStoragePath());
            BufferedImage originalImage = ImageIO.read(inputStream);
            inputStream.close();

            if (originalImage == null) {
                log.error("无法读取图片: {}", file.getStoragePath());
                return null;
            }

            // 计算缩略图尺寸
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            double ratio = Math.min(
                    (double) THUMBNAIL_MAX_WIDTH / originalWidth,
                    (double) THUMBNAIL_MAX_HEIGHT / originalHeight
            );

            // 如果原图已经很小，不需要缩放
            if (ratio >= 1.0) {
                ratio = 1.0;
            }

            int newWidth = (int) (originalWidth * ratio);
            int newHeight = (int) (originalHeight * ratio);

            // 生成缩略图
            BufferedImage thumbnail = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumbnail.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            // 保存缩略图
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", baos);
            byte[] thumbnailData = baos.toByteArray();

            String thumbnailPath = generateThumbnailPath(file.getStoragePath());
            minioService.upload(thumbnailPath, new ByteArrayInputStream(thumbnailData), 
                    thumbnailData.length, "image/jpeg");

            // 更新文件记录
            file.setThumbnailPath(thumbnailPath);
            file.setHasPreview(true);
            digitalFileMapper.updateById(file);

            log.info("缩略图生成成功: fileId={}, path={}", fileId, thumbnailPath);
            return thumbnailPath;

        } catch (Exception e) {
            log.error("生成缩略图失败: fileId={}", fileId, e);
            return null;
        }
    }

    @Override
    public String getPreviewUrl(Long fileId, String accessIp) {
        DigitalFile file = digitalFileMapper.selectById(fileId);
        if (file == null) {
            throw NotFoundException.of("文件", fileId);
        }

        // 记录预览日志
        accessLogService.logPreview(file.getArchiveId(), fileId, accessIp);

        String ext = file.getFileExtension();
        if (ext == null) {
            return null;
        }
        ext = ext.toLowerCase();

        // PDF和图片可以直接预览
        if (DIRECT_PREVIEWABLE.contains(ext)) {
            // 如果有预览路径，优先返回预览路径
            if (StringUtils.hasText(file.getPreviewPath())) {
                return minioService.getPresignedUrl(file.getPreviewPath(), 3600);
            }
            return minioService.getPresignedUrl(file.getStoragePath(), 3600);
        }

        // 其他文件类型暂不支持预览
        return null;
    }

    @Override
    public boolean isPreviewable(DigitalFile file) {
        if (file == null || file.getFileExtension() == null) {
            return false;
        }
        return isDirectPreviewable(file.getFileExtension());
    }

    @Override
    public boolean isDirectPreviewable(String fileExtension) {
        if (fileExtension == null) {
            return false;
        }
        return DIRECT_PREVIEWABLE.contains(fileExtension.toLowerCase());
    }

    private String generateThumbnailPath(String originalPath) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return String.format("thumbnails/%s/%s.jpg", date, uuid);
    }
}
