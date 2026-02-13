package com.lawfirm.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * 压缩工具类
 *
 * <p>提供 ZIP 压缩/解压功能，支持： - 单文件/多文件压缩 - 目录压缩 - 内存数据压缩 - 流式压缩
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public final class CompressUtils {

  /** 缓冲区大小. */
  private static final int BUFFER_SIZE = 8192;

  /** ZIP 扩展名. */
  private static final String ZIP_EXTENSION = ".zip";

  /** 字节掩码 0x50. */
  private static final int MASK_50 = 0x50;

  /** 字节掩码 0x4B. */
  private static final int MASK_4B = 0x4B;

  /** 字节掩码 0x03. */
  private static final int MASK_03 = 0x03;

  /** 字节掩码 0x05. */
  private static final int MASK_05 = 0x05;

  /** 字节掩码 0x07. */
  private static final int MASK_07 = 0x07;

  /** 字节掩码 0x04. */
  private static final int MASK_04 = 0x04;

  /** 字节掩码 0x06. */
  private static final int MASK_06 = 0x06;

  /** 字节掩码 0x08. */
  private static final int MASK_08 = 0x08;

  /** 文件头长度 4. */
  private static final int HEADER_LENGTH_4 = 4;

  private CompressUtils() {
    // 工具类禁止实例化
  }

  // ==================== ZIP 压缩 ====================

  /**
   * 压缩单个文件
   *
   * @param sourceFile 源文件
   * @param zipFile 目标 ZIP 文件
   * @throws IOException 压缩失败时抛出
   */
  public static void zipFile(final File sourceFile, final File zipFile) throws IOException {
    Assert.notNull(sourceFile, "源文件不能为空");
    Assert.notNull(zipFile, "目标文件不能为空");
    Assert.isTrue(sourceFile.exists(), "源文件不存在: " + sourceFile.getAbsolutePath());

    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
      addFileToZip(zos, sourceFile, sourceFile.getName());
      log.debug("文件压缩完成: {} -> {}", sourceFile.getName(), zipFile.getName());
    }
  }

  /**
   * 压缩多个文件到 ZIP
   *
   * @param sourceFiles 源文件列表
   * @param zipFile 目标 ZIP 文件
   * @throws IOException 压缩失败时抛出
   */
  public static void zipFiles(final List<File> sourceFiles, final File zipFile) throws IOException {
    Assert.notEmpty(sourceFiles, "源文件列表不能为空");
    Assert.notNull(zipFile, "目标文件不能为空");

    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
      for (File file : sourceFiles) {
        if (file != null && file.exists()) {
          addFileToZip(zos, file, file.getName());
        }
      }
      log.debug("批量文件压缩完成: {} 个文件 -> {}", sourceFiles.size(), zipFile.getName());
    }
  }

  /**
   * 压缩目录
   *
   * @param sourceDir 源目录
   * @param zipFile 目标 ZIP 文件
   * @throws IOException 压缩失败时抛出
   */
  public static void zipDirectory(final File sourceDir, final File zipFile) throws IOException {
    Assert.notNull(sourceDir, "源目录不能为空");
    Assert.notNull(zipFile, "目标文件不能为空");
    Assert.isTrue(
        sourceDir.exists() && sourceDir.isDirectory(),
        "源目录不存在或不是目录: " + sourceDir.getAbsolutePath());

    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
      addDirectoryToZip(zos, sourceDir, "");
      log.debug("目录压缩完成: {} -> {}", sourceDir.getName(), zipFile.getName());
    }
  }

  /**
   * 压缩内存数据到 ZIP（用于批量下载）
   *
   * @param dataMap 文件名 -> 文件内容 的映射
   * @param zipFile 目标 ZIP 文件
   * @throws IOException 压缩失败时抛出
   */
  public static void zipData(final Map<String, byte[]> dataMap, final File zipFile)
      throws IOException {
    Assert.notEmpty(dataMap, "数据映射不能为空");
    Assert.notNull(zipFile, "目标文件不能为空");

    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {
      for (Map.Entry<String, byte[]> entry : dataMap.entrySet()) {
        String fileName = entry.getKey();
        byte[] data = entry.getValue();
        if (fileName != null && data != null) {
          ZipEntry zipEntry = new ZipEntry(fileName);
          zos.putNextEntry(zipEntry);
          zos.write(data);
          zos.closeEntry();
        }
      }
      log.debug("内存数据压缩完成: {} 个文件 -> {}", dataMap.size(), zipFile.getName());
    }
  }

  /**
   * 压缩内存数据到字节数组（用于直接返回给前端下载）
   *
   * @param dataMap 文件名 -> 文件内容 的映射
   * @return ZIP 文件的字节数组
   * @throws IOException 压缩失败时抛出
   */
  public static byte[] zipDataToBytes(final Map<String, byte[]> dataMap) throws IOException {
    Assert.notEmpty(dataMap, "数据映射不能为空");

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
      for (Map.Entry<String, byte[]> entry : dataMap.entrySet()) {
        String fileName = entry.getKey();
        byte[] data = entry.getValue();
        if (fileName != null && data != null) {
          ZipEntry zipEntry = new ZipEntry(fileName);
          zos.putNextEntry(zipEntry);
          zos.write(data);
          zos.closeEntry();
        }
      }
      zos.finish();
      log.debug("内存数据压缩完成: {} 个文件", dataMap.size());
      return baos.toByteArray();
    }
  }

  /**
   * 压缩输入流到 ZIP（流式处理，适合大文件）
   *
   * @param streams 文件名 -> 输入流 的映射
   * @param output 输出流
   * @throws IOException 压缩失败时抛出
   */
  public static void zipStreams(final Map<String, InputStream> streams, final OutputStream output)
      throws IOException {
    Assert.notEmpty(streams, "输入流映射不能为空");
    Assert.notNull(output, "输出流不能为空");

    try (ZipOutputStream zos = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
      byte[] buffer = new byte[BUFFER_SIZE];
      for (Map.Entry<String, InputStream> entry : streams.entrySet()) {
        String fileName = entry.getKey();
        InputStream is = entry.getValue();
        if (fileName != null && is != null) {
          ZipEntry zipEntry = new ZipEntry(fileName);
          zos.putNextEntry(zipEntry);
          int len;
          while ((len = is.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
          }
          zos.closeEntry();
          is.close();
        }
      }
      log.debug("流式压缩完成: {} 个文件", streams.size());
    }
  }

  // ==================== ZIP 解压 ====================

  /**
   * 解压 ZIP 文件到目录
   *
   * @param zipFile ZIP 文件
   * @param targetDir 目标目录
   * @throws IOException 解压失败时抛出
   */
  public static void unzip(final File zipFile, final File targetDir) throws IOException {
    Assert.notNull(zipFile, "ZIP 文件不能为空");
    Assert.notNull(targetDir, "目标目录不能为空");
    Assert.isTrue(zipFile.exists(), "ZIP 文件不存在: " + zipFile.getAbsolutePath());

    if (!targetDir.exists()) {
      targetDir.mkdirs();
    }

    try (ZipInputStream zis =
        new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      byte[] buffer = new byte[BUFFER_SIZE];
      while ((entry = zis.getNextEntry()) != null) {
        File destFile = newFile(targetDir, entry);
        if (entry.isDirectory()) {
          destFile.mkdirs();
        } else {
          // 确保父目录存在
          File parent = destFile.getParentFile();
          if (!parent.exists()) {
            parent.mkdirs();
          }
          try (FileOutputStream fos = new FileOutputStream(destFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        zis.closeEntry();
      }
      log.debug("ZIP 解压完成: {} -> {}", zipFile.getName(), targetDir.getAbsolutePath());
    }
  }

  /**
   * 解压 ZIP 字节数组到目录
   *
   * @param zipData ZIP 数据
   * @param targetDir 目标目录
   * @throws IOException 解压失败时抛出
   */
  public static void unzip(final byte[] zipData, final File targetDir) throws IOException {
    Assert.notNull(zipData, "ZIP 数据不能为空");
    Assert.notNull(targetDir, "目标目录不能为空");

    if (!targetDir.exists()) {
      targetDir.mkdirs();
    }

    try (ZipInputStream zis =
        new ZipInputStream(new ByteArrayInputStream(zipData), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      byte[] buffer = new byte[BUFFER_SIZE];
      while ((entry = zis.getNextEntry()) != null) {
        File destFile = newFile(targetDir, entry);
        if (entry.isDirectory()) {
          destFile.mkdirs();
        } else {
          File parent = destFile.getParentFile();
          if (!parent.exists()) {
            parent.mkdirs();
          }
          try (FileOutputStream fos = new FileOutputStream(destFile)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
        }
        zis.closeEntry();
      }
      log.debug("ZIP 字节数组解压完成 -> {}", targetDir.getAbsolutePath());
    }
  }

  // ==================== GZIP 压缩 ====================

  /**
   * GZIP 压缩字节数组
   *
   * @param data 原始数据
   * @return 压缩后的数据
   * @throws IOException 压缩失败时抛出
   */
  public static byte[] gzip(final byte[] data) throws IOException {
    Assert.notNull(data, "数据不能为空");

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
      gzos.write(data);
      gzos.finish();
      return baos.toByteArray();
    }
  }

  /**
   * GZIP 压缩字符串
   *
   * @param str 原始字符串
   * @return 压缩后的数据
   * @throws IOException 压缩失败时抛出
   */
  public static byte[] gzip(final String str) throws IOException {
    Assert.notBlank(str, "字符串不能为空");
    return gzip(str.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * GZIP 解压字节数组
   *
   * @param data 压缩数据
   * @return 解压后的数据
   * @throws IOException 解压失败时抛出
   */
  public static byte[] ungzip(final byte[] data) throws IOException {
    Assert.notNull(data, "数据不能为空");

    try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
        GZIPInputStream gzis = new GZIPInputStream(bais);
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[BUFFER_SIZE];
      int len;
      while ((len = gzis.read(buffer)) > 0) {
        baos.write(buffer, 0, len);
      }
      return baos.toByteArray();
    }
  }

  /**
   * GZIP 解压为字符串
   *
   * @param data 压缩数据
   * @return 解压后的字符串
   * @throws IOException 解压失败时抛出
   */
  public static String ungzipToString(final byte[] data) throws IOException {
    return new String(ungzip(data), StandardCharsets.UTF_8);
  }

  // ==================== 辅助方法 ====================

  /**
   * 添加文件到 ZIP
   *
   * @param zos ZIP输出流
   * @param file 文件
   * @param entryName 条目名称
   * @throws IOException IO异常
   */
  private static void addFileToZip(
      final ZipOutputStream zos, final File file, final String entryName) throws IOException {
    if (file.isDirectory()) {
      addDirectoryToZip(zos, file, entryName);
    } else {
      ZipEntry zipEntry = new ZipEntry(entryName);
      zos.putNextEntry(zipEntry);
      try (FileInputStream fis = new FileInputStream(file)) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = fis.read(buffer)) > 0) {
          zos.write(buffer, 0, len);
        }
      }
      zos.closeEntry();
    }
  }

  /**
   * 添加目录到 ZIP
   *
   * @param zos ZIP输出流
   * @param dir 目录
   * @param basePath 基础路径
   * @throws IOException IO异常
   */
  private static void addDirectoryToZip(
      final ZipOutputStream zos, final File dir, final String basePath) throws IOException {
    File[] files = dir.listFiles();
    if (files == null || files.length == 0) {
      // 空目录也要添加
      if (!basePath.isEmpty()) {
        ZipEntry zipEntry = new ZipEntry(basePath + "/");
        zos.putNextEntry(zipEntry);
        zos.closeEntry();
      }
      return;
    }

    for (File file : files) {
      String entryName = basePath.isEmpty() ? file.getName() : basePath + "/" + file.getName();
      addFileToZip(zos, file, entryName);
    }
  }

  /**
   * 安全创建目标文件（防止 Zip Slip 漏洞）
   *
   * @param targetDir 目标目录
   * @param entry ZIP条目
   * @return 目标文件
   * @throws IOException IO异常
   */
  private static File newFile(final File targetDir, final ZipEntry entry) throws IOException {
    File destFile = new File(targetDir, entry.getName());
    String destDirPath = targetDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("ZIP 条目在目标目录之外: " + entry.getName());
    }

    return destFile;
  }

  /**
   * 检查文件是否为 ZIP 文件
   *
   * @param file 文件
   * @return 是否为 ZIP 文件
   */
  public static boolean isZipFile(final File file) {
    if (file == null || !file.exists() || !file.isFile()) {
      return false;
    }

    // 检查文件头魔数 (PK..)
    try (FileInputStream fis = new FileInputStream(file)) {
      byte[] header = new byte[HEADER_LENGTH_4];
      if (fis.read(header) == HEADER_LENGTH_4) {
        return header[0] == MASK_50
            && header[1] == MASK_4B
            && (header[2] == MASK_03 || header[2] == MASK_05 || header[2] == MASK_07)
            && (header[3] == MASK_04 || header[3] == MASK_06 || header[3] == MASK_08);
      }
    } catch (IOException e) {
      log.warn("检查 ZIP 文件失败: {}", file.getName(), e);
    }
    return false;
  }

  /**
   * 获取 ZIP 文件中的条目列表
   *
   * @param zipFile ZIP 文件
   * @return 条目名称列表
   * @throws IOException 读取失败时抛出
   */
  public static List<String> listZipEntries(final File zipFile) throws IOException {
    Assert.notNull(zipFile, "ZIP 文件不能为空");
    Assert.isTrue(zipFile.exists(), "ZIP 文件不存在");

    List<String> entries = new ArrayList<>();
    try (ZipInputStream zis =
        new ZipInputStream(new FileInputStream(zipFile), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        entries.add(entry.getName());
        zis.closeEntry();
      }
    }
    return entries;
  }

  /**
   * 生成 ZIP 文件名（添加 .zip 后缀）
   *
   * @param baseName 基础名称
   * @return 带 .zip 后缀的文件名
   */
  public static String toZipFileName(final String baseName) {
    if (baseName == null || baseName.isEmpty()) {
      return "archive" + ZIP_EXTENSION;
    }
    if (baseName.toLowerCase().endsWith(ZIP_EXTENSION)) {
      return baseName;
    }
    return baseName + ZIP_EXTENSION;
  }
}
