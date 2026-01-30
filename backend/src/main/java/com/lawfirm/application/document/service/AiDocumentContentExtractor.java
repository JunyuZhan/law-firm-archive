package com.lawfirm.application.document.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.lawfirm.domain.document.entity.Document;
import com.lawfirm.infrastructure.external.minio.MinioService;
import com.lawfirm.infrastructure.ocr.OcrResult;
import com.lawfirm.infrastructure.ocr.OcrService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI 文档内容提取服务 用于 AI 文书生成时提取项目文档内容 支持提取多种文档格式的文本内容： - Word: .doc, .docx - PDF: .pdf - 文本: .txt - 图片:
 * .jpg, .jpeg, .png, .gif, .bmp (通过 OCR)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiDocumentContentExtractor {

  /** MinIO服务. */
  private final MinioService minioService;

  /** OCR服务. */
  private final OcrService ocrService;

  /** 内容提取的最大字符数（避免超大文档）. */
  private static final int MAX_CONTENT_LENGTH = 10000;

  /** 支持的Word文件类型. */
  private static final Set<String> WORD_TYPES = new HashSet<>(Arrays.asList("doc", "docx"));

  /** 支持的PDF文件类型. */
  private static final Set<String> PDF_TYPES = new HashSet<>(Arrays.asList("pdf"));

  /** 支持的文本文件类型. */
  private static final Set<String> TEXT_TYPES = new HashSet<>(Arrays.asList("txt", "text"));

  /** 支持的图片文件类型. */
  private static final Set<String> IMAGE_TYPES =
      new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp"));

  /**
   * 提取文档内容。
   *
   * @param document 文档实体
   * @return 文档文本内容，提取失败返回 null
   */
  public String extractContent(final Document document) {
    if (document == null || document.getFilePath() == null) {
      return null;
    }

    String fileType = document.getFileType();
    if (fileType == null) {
      fileType = getFileExtension(document.getFileName());
    }
    fileType = fileType.toLowerCase();

    try {
      if (WORD_TYPES.contains(fileType)) {
        return fileType.equals("docx")
            ? extractDocxContent(document.getFilePath())
            : extractDocContent(document.getFilePath());
      } else if (PDF_TYPES.contains(fileType)) {
        return extractPdfContent(document.getFilePath());
      } else if (TEXT_TYPES.contains(fileType)) {
        return extractTxtContent(document.getFilePath());
      } else if (IMAGE_TYPES.contains(fileType)) {
        return extractImageContent(document.getFilePath(), document.getFileName(), fileType);
      } else {
        log.debug("不支持提取的文件类型: {}", fileType);
        return null;
      }
    } catch (Exception e) {
      log.error("提取文档内容失败: {} - {}", document.getFileName(), e.getMessage());
      return null;
    }
  }

  /**
   * 提取 .docx 文档内容。
   *
   * @param filePath 文件路径
   * @return 文档文本内容
   * @throws Exception 提取失败时抛出异常
   */
  private String extractDocxContent(final String filePath) throws Exception {
    try (InputStream is = minioService.downloadFile(filePath);
        XWPFDocument doc = new XWPFDocument(is)) {

      StringBuilder content = new StringBuilder();
      List<XWPFParagraph> paragraphs = doc.getParagraphs();

      for (XWPFParagraph para : paragraphs) {
        String text = para.getText();
        if (text != null && !text.trim().isEmpty()) {
          content.append(text.trim()).append("\n");
        }

        // 限制内容长度
        if (content.length() > MAX_CONTENT_LENGTH) {
          content.append("\n... [内容过长，已截断]");
          break;
        }
      }

      return content.toString().trim();
    }
  }

  /**
   * 提取 .doc 文档内容。
   *
   * @param filePath 文件路径
   * @return 文档文本内容
   * @throws Exception 提取失败时抛出异常
   */
  private String extractDocContent(final String filePath) throws Exception {
    try (InputStream is = minioService.downloadFile(filePath);
        HWPFDocument doc = new HWPFDocument(is);
        WordExtractor extractor = new WordExtractor(doc)) {

      String text = extractor.getText();

      // 限制内容长度
      if (text.length() > MAX_CONTENT_LENGTH) {
        text = text.substring(0, MAX_CONTENT_LENGTH) + "\n... [内容过长，已截断]";
      }

      return text.trim();
    }
  }

  /**
   * 提取 PDF 文档内容。
   *
   * @param filePath 文件路径
   * @return 文档文本内容
   * @throws Exception 提取失败时抛出异常
   */
  private String extractPdfContent(final String filePath) throws Exception {
    try (InputStream is = minioService.downloadFile(filePath);
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(is))) {

      StringBuilder content = new StringBuilder();
      int pageCount = pdfDoc.getNumberOfPages();

      for (int i = 1; i <= pageCount; i++) {
        String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i));
        if (pageText != null && !pageText.trim().isEmpty()) {
          content.append(pageText.trim()).append("\n\n");
        }

        // 限制内容长度
        if (content.length() > MAX_CONTENT_LENGTH) {
          content.append("\n... [内容过长，已截断]");
          break;
        }
      }

      return content.toString().trim();
    }
  }

  /**
   * 提取 .txt 文档内容。
   *
   * @param filePath 文件路径
   * @return 文档文本内容
   * @throws Exception 提取失败时抛出异常
   */
  private String extractTxtContent(final String filePath) throws Exception {
    try (InputStream is = minioService.downloadFile(filePath)) {
      byte[] bytes = is.readAllBytes();
      String text = new String(bytes, "UTF-8");

      // 限制内容长度
      if (text.length() > MAX_CONTENT_LENGTH) {
        text = text.substring(0, MAX_CONTENT_LENGTH) + "\n... [内容过长，已截断]";
      }

      return text.trim();
    }
  }

  /**
   * 提取图片文档内容（通过 OCR）。
   *
   * @param filePath 文件路径
   * @param fileName 文件名
   * @param fileType 文件类型
   * @return 识别的文本内容
   * @throws Exception 提取失败时抛出异常
   */
  private String extractImageContent(
      final String filePath, final String fileName, final String fileType) throws Exception {
    try (InputStream is = minioService.downloadFile(filePath)) {
      byte[] imageBytes = is.readAllBytes();

      // 构建 MultipartFile 使用简单的实现
      String contentType = getImageContentType(fileType);
      MultipartFile multipartFile = new SimpleMultipartFile(fileName, contentType, imageBytes);

      // 调用 OCR 服务识别
      OcrResult result = ocrService.recognizeText(multipartFile);

      if (result.isSuccess() && result.getRawText() != null) {
        String text = result.getRawText();

        // 限制内容长度
        if (text.length() > MAX_CONTENT_LENGTH) {
          text = text.substring(0, MAX_CONTENT_LENGTH) + "\n... [内容过长，已截断]";
        }

        return text.trim();
      } else {
        log.warn("OCR 识别失败: {}", result.getErrorMessage());
        return null;
      }
    }
  }

  /** 简单的 MultipartFile 实现。 */
  private static class SimpleMultipartFile implements MultipartFile {
    /** 文件名. */
    private final String name;

    /** 内容类型. */
    private final String contentType;

    /** 文件内容. */
    private final byte[] content;

    /**
     * 构造函数。
     *
     * @param name 文件名
     * @param contentType 内容类型
     * @param content 文件内容
     */
    SimpleMultipartFile(final String name, final String contentType, final byte[] content) {
      this.name = name;
      this.contentType = contentType;
      this.content = content;
    }

    /**
     * 获取文件名。
     *
     * @return 文件名
     */
    @Override
    public String getName() {
      return name;
    }

    /**
     * 获取原始文件名。
     *
     * @return 原始文件名
     */
    @Override
    public String getOriginalFilename() {
      return name;
    }

    /**
     * 获取内容类型。
     *
     * @return 内容类型
     */
    @Override
    public String getContentType() {
      return contentType;
    }

    /**
     * 判断文件是否为空。
     *
     * @return 是否为空
     */
    @Override
    public boolean isEmpty() {
      return content == null || content.length == 0;
    }

    /**
     * 获取文件大小。
     *
     * @return 文件大小（字节）
     */
    @Override
    public long getSize() {
      return content != null ? content.length : 0;
    }

    /**
     * 获取文件字节数组。
     *
     * @return 文件字节数组
     */
    @Override
    public byte[] getBytes() {
      return content;
    }

    /**
     * 获取输入流。
     *
     * @return 输入流
     */
    @Override
    public InputStream getInputStream() {
      return new ByteArrayInputStream(content);
    }

    /**
     * 将文件传输到目标位置。
     *
     * @param dest 目标文件
     * @throws java.io.IOException IO异常
     */
    @Override
    public void transferTo(final java.io.File dest) throws java.io.IOException {
      java.nio.file.Files.write(dest.toPath(), content);
    }
  }

  /**
   * 获取图片 MIME 类型。
   *
   * @param fileType 文件类型
   * @return MIME类型
   */
  private String getImageContentType(final String fileType) {
    return switch (fileType.toLowerCase()) {
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "gif" -> "image/gif";
      case "bmp" -> "image/bmp";
      case "webp" -> "image/webp";
      default -> "image/jpeg";
    };
  }

  /**
   * 获取文件扩展名。
   *
   * @param fileName 文件名
   * @return 文件扩展名
   */
  private String getFileExtension(final String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "";
    }
    return fileName.substring(fileName.lastIndexOf(".") + 1);
  }

  /**
   * 判断是否支持提取该文档类型。
   *
   * @param fileType 文件类型
   * @return 是否支持
   */
  public boolean isSupported(final String fileType) {
    if (fileType == null) {
      return false;
    }
    String type = fileType.toLowerCase();
    return WORD_TYPES.contains(type)
        || PDF_TYPES.contains(type)
        || TEXT_TYPES.contains(type)
        || IMAGE_TYPES.contains(type);
  }

  /**
   * 获取支持的文件类型描述。
   *
   * @return 支持的文件类型描述
   */
  public String getSupportedTypesDescription() {
    return "Word (.doc, .docx), PDF (.pdf), 文本 (.txt), 图片 (.jpg, .jpeg, .png, .gif, .bmp, .webp)";
  }
}
