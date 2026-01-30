package com.lawfirm.infrastructure.external.file;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件类型服务.
 *
 * <p>负责文件类型识别、验证和分类
 *
 * @author system
 * @since 2026-01-10
 */
@Slf4j
@Service
public class FileTypeService {

  /** 最大文件大小：100MB. */
  public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;

  /** 支持的文件扩展名及其类型映射. */
  private static final Map<String, FileTypeInfo> FILE_TYPE_MAP =
      Map.ofEntries(
          // 图片
          Map.entry("jpg", new FileTypeInfo("image", "FileImageOutlined", true, true)),
          Map.entry("jpeg", new FileTypeInfo("image", "FileImageOutlined", true, true)),
          Map.entry("png", new FileTypeInfo("image", "FileImageOutlined", true, true)),
          Map.entry("gif", new FileTypeInfo("image", "FileImageOutlined", true, true)),
          Map.entry("bmp", new FileTypeInfo("image", "FileImageOutlined", true, true)),
          Map.entry("webp", new FileTypeInfo("image", "FileImageOutlined", true, true)),

          // PDF
          Map.entry("pdf", new FileTypeInfo("pdf", "FilePdfOutlined", true, false)),

          // Word
          Map.entry("doc", new FileTypeInfo("word", "FileWordOutlined", true, false)),
          Map.entry("docx", new FileTypeInfo("word", "FileWordOutlined", true, false)),

          // Excel
          Map.entry("xls", new FileTypeInfo("excel", "FileExcelOutlined", true, false)),
          Map.entry("xlsx", new FileTypeInfo("excel", "FileExcelOutlined", true, false)),

          // PPT
          Map.entry("ppt", new FileTypeInfo("ppt", "FilePptOutlined", true, false)),
          Map.entry("pptx", new FileTypeInfo("ppt", "FilePptOutlined", true, false)),

          // 音频
          Map.entry("mp3", new FileTypeInfo("audio", "AudioOutlined", true, false)),
          Map.entry("wav", new FileTypeInfo("audio", "AudioOutlined", true, false)),
          Map.entry("m4a", new FileTypeInfo("audio", "AudioOutlined", true, false)),
          Map.entry("aac", new FileTypeInfo("audio", "AudioOutlined", true, false)),

          // 视频
          Map.entry("mp4", new FileTypeInfo("video", "VideoCameraOutlined", true, false)),
          Map.entry("avi", new FileTypeInfo("video", "VideoCameraOutlined", true, false)),
          Map.entry("mov", new FileTypeInfo("video", "VideoCameraOutlined", true, false)),
          Map.entry("wmv", new FileTypeInfo("video", "VideoCameraOutlined", true, false)));

  /** 支持的MIME类型. */
  @SuppressWarnings("unused")
  private static final Set<String> SUPPORTED_MIME_TYPES =
      Set.of(
          // 图片
          "image/jpeg",
          "image/png",
          "image/gif",
          "image/bmp",
          "image/webp",
          // PDF
          "application/pdf",
          // Word
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          // Excel
          "application/vnd.ms-excel",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          // PPT
          "application/vnd.ms-powerpoint",
          "application/vnd.openxmlformats-officedocument.presentationml.presentation",
          // 音频
          "audio/mpeg",
          "audio/wav",
          "audio/mp4",
          "audio/aac",
          "audio/x-m4a",
          // 视频
          "video/mp4",
          "video/x-msvideo",
          "video/quicktime",
          "video/x-ms-wmv");

  /**
   * 获取文件扩展名
   *
   * @param fileName 文件名
   * @return 文件扩展名
   */
  public String getFileExtension(final String fileName) {
    if (fileName == null || !fileName.contains(".")) {
      return "";
    }
    return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
  }

  /**
   * 获取文件类型信息
   *
   * @param fileName 文件名
   * @return 文件类型信息
   */
  public FileTypeInfo getFileTypeInfo(final String fileName) {
    if (fileName == null) {
      return new FileTypeInfo("other", "FileOutlined", false, false);
    }
    String extension = getFileExtension(fileName);
    return FILE_TYPE_MAP.getOrDefault(
        extension, new FileTypeInfo("other", "FileOutlined", false, false));
  }

  /**
   * 判断文件类型是否支持
   *
   * @param fileName 文件名
   * @return 是否支持
   */
  public boolean isSupported(final String fileName) {
    if (fileName == null) {
      return false;
    }
    String extension = getFileExtension(fileName);
    return FILE_TYPE_MAP.containsKey(extension);
  }

  /**
   * 判断是否为图片文件
   *
   * @param fileName 文件名
   * @return 是否为图片文件
   */
  public boolean isImageFile(final String fileName) {
    if (fileName == null) {
      return false;
    }
    FileTypeInfo info = getFileTypeInfo(fileName);
    return "image".equals(info.getType());
  }

  /**
   * 判断是否为视频文件
   *
   * @param fileName 文件名
   * @return 是否为视频文件
   */
  public boolean isVideoFile(final String fileName) {
    if (fileName == null) {
      return false;
    }
    FileTypeInfo info = getFileTypeInfo(fileName);
    return "video".equals(info.getType());
  }

  /**
   * 判断是否为音频文件
   *
   * @param fileName 文件名
   * @return 是否为音频文件
   */
  public boolean isAudioFile(final String fileName) {
    if (fileName == null) {
      return false;
    }
    FileTypeInfo info = getFileTypeInfo(fileName);
    return "audio".equals(info.getType());
  }

  /**
   * 判断是否为文档文件
   *
   * @param fileName 文件名
   * @return 是否为文档文件
   */
  public boolean isDocumentFile(final String fileName) {
    if (fileName == null) {
      return false;
    }
    FileTypeInfo info = getFileTypeInfo(fileName);
    String type = info.getType();
    return "pdf".equals(type) || "word".equals(type) || "excel".equals(type) || "ppt".equals(type);
  }

  /**
   * 验证文件
   *
   * @param file 文件
   * @return 验证结果，null表示验证通过，否则返回错误信息
   */
  public String validateFile(final MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return "文件不能为空";
    }

    // 检查文件大小
    if (file.getSize() > MAX_FILE_SIZE) {
      return "文件大小超过限制(100MB)";
    }

    // 检查文件类型
    String fileName = file.getOriginalFilename();
    if (!isSupported(fileName)) {
      return "不支持的文件类型，支持的类型：图片(jpg/png/gif/bmp/webp)、"
          + "文档(pdf/doc/docx/xls/xlsx/ppt/pptx)、音频(mp3/wav/m4a/aac)、"
          + "视频(mp4/avi/mov/wmv)";
    }

    return null; // 验证通过
  }

  /**
   * 获取所有支持的文件扩展名.
   *
   * @return 支持的文件扩展名集合
   */
  public Set<String> getSupportedExtensions() {
    return FILE_TYPE_MAP.keySet();
  }

  /** 文件类型信息. */
  public static class FileTypeInfo {
    /** 文件类型 */
    private final String type;

    /** 图标 */
    private final String icon;

    /** 是否可预览 */
    private final boolean canPreview;

    /** 是否可生成缩略图 */
    private final boolean canGenerateThumbnail;

    /**
     * 构造函数
     *
     * @param type 文件类型
     * @param icon 图标
     * @param canPreview 是否可预览
     * @param canGenerateThumbnail 是否可生成缩略图
     */
    public FileTypeInfo(
        final String type,
        final String icon,
        final boolean canPreview,
        final boolean canGenerateThumbnail) {
      this.type = type;
      this.icon = icon;
      this.canPreview = canPreview;
      this.canGenerateThumbnail = canGenerateThumbnail;
    }

    /**
     * 获取文件类型
     *
     * @return 文件类型
     */
    public String getType() {
      return type;
    }

    /**
     * 获取图标
     *
     * @return 图标
     */
    public String getIcon() {
      return icon;
    }

    /**
     * 是否可预览
     *
     * @return 是否可预览
     */
    public boolean isCanPreview() {
      return canPreview;
    }

    /**
     * 是否可生成缩略图
     *
     * @return 是否可生成缩略图
     */
    public boolean isCanGenerateThumbnail() {
      return canGenerateThumbnail;
    }
  }
}
