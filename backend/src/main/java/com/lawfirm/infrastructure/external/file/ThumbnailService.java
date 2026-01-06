package com.lawfirm.infrastructure.external.file;

import com.lawfirm.infrastructure.external.minio.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * 缩略图服务
 * 负责生成图片缩略图
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final MinioService minioService;
    private final FileTypeService fileTypeService;

    // 缩略图默认尺寸
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    // 缩略图存储目录
    private static final String THUMBNAIL_FOLDER = "thumbnails/";

    /**
     * 为上传的图片生成缩略图
     * @param file 原始图片文件
     * @param originalFileUrl 原始文件URL
     * @return 缩略图URL，如果生成失败返回null
     */
    public String generateThumbnail(MultipartFile file, String originalFileUrl) {
        String fileName = file.getOriginalFilename();
        
        // 只为图片文件生成缩略图
        if (!fileTypeService.isImageFile(fileName)) {
            log.debug("非图片文件，跳过缩略图生成: {}", fileName);
            return null;
        }

        try {
            return generateThumbnailFromStream(file.getInputStream(), fileName);
        } catch (Exception e) {
            log.error("生成缩略图失败: {}", fileName, e);
            return null;
        }
    }

    /**
     * 从输入流生成缩略图
     */
    public String generateThumbnailFromStream(InputStream inputStream, String originalFileName) {
        try {
            // 生成缩略图
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(inputStream)
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toOutputStream(outputStream);

            // 生成缩略图文件名
            String thumbnailFileName = UUID.randomUUID().toString() + "_thumb.jpg";

            // 上传缩略图到MinIO
            ByteArrayInputStream thumbnailStream = new ByteArrayInputStream(outputStream.toByteArray());
            String thumbnailUrl = minioService.uploadFile(
                    thumbnailStream, 
                    thumbnailFileName, 
                    THUMBNAIL_FOLDER, 
                    "image/jpeg"
            );

            log.info("缩略图生成成功: {} -> {}", originalFileName, thumbnailUrl);
            return thumbnailUrl;
        } catch (Exception e) {
            log.error("生成缩略图失败: {}", originalFileName, e);
            return null;
        }
    }

    /**
     * 从URL生成缩略图（用于已上传的文件）
     */
    public String generateThumbnailFromUrl(String fileUrl, String fileName) {
        if (!fileTypeService.isImageFile(fileName)) {
            return null;
        }

        try {
            String objectName = minioService.extractObjectName(fileUrl);
            if (objectName == null) {
                log.warn("无法从URL提取对象名称: {}", fileUrl);
                return null;
            }

            InputStream inputStream = minioService.downloadFile(objectName);
            return generateThumbnailFromStream(inputStream, fileName);
        } catch (Exception e) {
            log.error("从URL生成缩略图失败: {}", fileUrl, e);
            return null;
        }
    }
}
