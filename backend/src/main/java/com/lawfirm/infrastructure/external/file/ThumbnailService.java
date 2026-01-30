package com.lawfirm.infrastructure.external.file;

import com.lawfirm.infrastructure.external.minio.MinioService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 缩略图服务 负责生成图片和PDF文件的缩略图 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

  /** MinIO服务 */
  private final MinioService minioService;

  /** 文件类型服务 */
  private final FileTypeService fileTypeService;

  /** 缩略图默认宽度（提高分辨率以支持更清晰的预览） */
  private static final int THUMBNAIL_WIDTH = 480;

  /** 缩略图默认高度 */
  private static final int THUMBNAIL_HEIGHT = 480;

  /** 缩略图存储目录 */
  private static final String THUMBNAIL_FOLDER = "thumbnails/";

  /** 支持缩略图的PDF扩展名 */
  private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");

  /** 图片质量 */
  private static final double IMAGE_QUALITY = 0.8;

  /** PDF渲染DPI */
  private static final int PDF_RENDER_DPI = 150;

  /**
   * 为上传的文件生成缩略图（支持图片和PDF） 问题247修复：确保InputStream正确关闭
   *
   * @param file 原始文件
   * @param originalFileUrl 原始文件URL
   * @return 缩略图URL，如果生成失败返回null
   */
  public String generateThumbnail(final MultipartFile file, final String originalFileUrl) {
    String fileName = file.getOriginalFilename();

    // 图片文件 - 使用 Thumbnailator
    if (fileTypeService.isImageFile(fileName)) {
      try (InputStream inputStream = file.getInputStream()) {
        return generateThumbnailFromStream(inputStream, fileName);
      } catch (Exception e) {
        log.error("生成图片缩略图失败: {}", fileName, e);
        return null;
      }
    }

    // PDF文件 - 使用 PDFBox
    if (isPdfFile(fileName)) {
      try (InputStream inputStream = file.getInputStream()) {
        return generatePdfThumbnail(inputStream, fileName);
      } catch (Exception e) {
        log.error("生成PDF缩略图失败: {}", fileName, e);
        return null;
      }
    }

    log.debug("不支持的文件类型，跳过缩略图生成: {}", fileName);
    return null;
  }

  /**
   * 判断是否为PDF文件.
   *
   * @param fileName 文件名
   * @return 是否为PDF文件
   */
  public boolean isPdfFile(final String fileName) {
    if (fileName == null) {
      return false;
    }
    String ext = fileName.toLowerCase();
    int dotIndex = ext.lastIndexOf('.');
    if (dotIndex > 0) {
      ext = ext.substring(dotIndex + 1);
    }
    return PDF_EXTENSIONS.contains(ext);
  }

  /**
   * 判断文件是否支持生成缩略图
   *
   * @param fileName 文件名
   * @return 是否支持生成缩略图
   */
  public boolean supportsThumbnail(final String fileName) {
    return fileTypeService.isImageFile(fileName) || isPdfFile(fileName);
  }

  /**
   * 从输入流生成图片缩略图 ⚠️ 注意：此方法不会关闭 inputStream，调用者需要负责关闭
   *
   * @param inputStream 输入流
   * @param originalFileName 原始文件名
   * @return 缩略图URL
   */
  public String generateThumbnailFromStream(
      final InputStream inputStream, final String originalFileName) {
    try {
      // 生成缩略图
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Thumbnails.of(inputStream)
          .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
          .keepAspectRatio(true)
          .outputFormat("jpg")
          .outputQuality(IMAGE_QUALITY)
          .toOutputStream(outputStream);

      // 生成缩略图文件名
      String thumbnailFileName = UUID.randomUUID().toString() + "_thumb.jpg";

      // 上传缩略图到MinIO
      try (ByteArrayInputStream thumbnailStream =
          new ByteArrayInputStream(outputStream.toByteArray())) {
        String thumbnailUrl =
            minioService.uploadFile(
                thumbnailStream, thumbnailFileName, THUMBNAIL_FOLDER, "image/jpeg");

        log.info("图片缩略图生成成功: {} -> {}", originalFileName, thumbnailUrl);
        return thumbnailUrl;
      }
    } catch (Exception e) {
      log.error("生成图片缩略图失败: {}", originalFileName, e);
      return null;
    }
  }

  /**
   * 生成PDF文件的缩略图（提取第一页）
   *
   * @param inputStream PDF文件输入流
   * @param originalFileName 原始文件名
   * @return 缩略图URL
   */
  public String generatePdfThumbnail(final InputStream inputStream, final String originalFileName) {
    try {
      // 读取PDF到字节数组（PDFBox 3.x 需要）
      byte[] pdfBytes = inputStream.readAllBytes();

      try (PDDocument document = Loader.loadPDF(pdfBytes)) {
        if (document.getNumberOfPages() == 0) {
          log.warn("PDF文件没有页面: {}", originalFileName);
          return null;
        }

        // 渲染第一页为图片
        PDFRenderer renderer = new PDFRenderer(document);
        // 使用150 DPI渲染，保证缩略图清晰度
        BufferedImage pageImage = renderer.renderImageWithDPI(0, PDF_RENDER_DPI);

        // 使用 Thumbnailator 调整大小
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(pageImage)
            .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
            .keepAspectRatio(true)
            .outputFormat("jpg")
            .outputQuality(IMAGE_QUALITY)
            .toOutputStream(outputStream);

        // 生成缩略图文件名
        String thumbnailFileName = UUID.randomUUID().toString() + "_pdf_thumb.jpg";

        // 上传缩略图到MinIO
        try (ByteArrayInputStream thumbnailStream =
            new ByteArrayInputStream(outputStream.toByteArray())) {
          String thumbnailUrl =
              minioService.uploadFile(
                  thumbnailStream, thumbnailFileName, THUMBNAIL_FOLDER, "image/jpeg");

          log.info("PDF缩略图生成成功: {} -> {}", originalFileName, thumbnailUrl);
          return thumbnailUrl;
        }
      }
    } catch (Exception e) {
      log.error("生成PDF缩略图失败: {}", originalFileName, e);
      return null;
    }
  }

  /**
   * 从URL生成缩略图（用于已上传的文件）
   *
   * @param fileUrl 文件URL
   * @param fileName 文件名
   * @return 缩略图URL
   */
  public String generateThumbnailFromUrl(final String fileUrl, final String fileName) {
    // 图片文件
    if (fileTypeService.isImageFile(fileName)) {
      try {
        String objectName = minioService.extractObjectName(fileUrl);
        if (objectName == null) {
          log.warn("无法从URL提取对象名称: {}", fileUrl);
          return null;
        }

        try (InputStream inputStream = minioService.downloadFile(objectName)) {
          return generateThumbnailFromStream(inputStream, fileName);
        }
      } catch (Exception e) {
        log.error("从URL生成图片缩略图失败: {}", fileUrl, e);
        return null;
      }
    }

    // PDF文件
    if (isPdfFile(fileName)) {
      try {
        String objectName = minioService.extractObjectName(fileUrl);
        if (objectName == null) {
          log.warn("无法从URL提取对象名称: {}", fileUrl);
          return null;
        }

        try (InputStream inputStream = minioService.downloadFile(objectName)) {
          return generatePdfThumbnail(inputStream, fileName);
        }
      } catch (Exception e) {
        log.error("从URL生成PDF缩略图失败: {}", fileUrl, e);
        return null;
      }
    }

    return null;
  }
}
