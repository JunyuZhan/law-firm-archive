package com.lawfirm.common.util;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件哈希工具类.
 *
 * <p>提供计算和验证文件哈希值的功能
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
public final class FileHashUtil {
  /** Hash算法 */
  private static final String HASH_ALGORITHM = "SHA-256";

  /** 缓冲区大小 */
  private static final int BUFFER_SIZE = 8192;

  private FileHashUtil() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * 计算文件Hash值
   *
   * @param file 文件
   * @return Hash值
   */
  public static String calculateHash(final MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    try (InputStream inputStream = file.getInputStream()) {
      return calculateHash(inputStream);
    } catch (Exception e) {
      log.error("计算文件Hash失败: {}", file.getOriginalFilename(), e);
      return null;
    }
  }

  /**
   * 计算输入流Hash值
   *
   * @param inputStream 输入流
   * @return Hash值
   */
  public static String calculateHash(final InputStream inputStream) {
    if (inputStream == null) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        digest.update(buffer, 0, bytesRead);
      }
      byte[] hashBytes = digest.digest();
      return bytesToHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Hash算法不存在: {}", HASH_ALGORITHM, e);
      return null;
    } catch (Exception e) {
      log.error("计算Hash失败", e);
      return null;
    }
  }

  /**
   * 计算字节数组Hash值
   *
   * @param bytes 字节数组
   * @return Hash值
   */
  public static String calculateHash(final byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hashBytes = digest.digest(bytes);
      return bytesToHex(hashBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Hash算法不存在: {}", HASH_ALGORITHM, e);
      return null;
    }
  }

  /**
   * 验证文件Hash值
   *
   * @param file 文件
   * @param expectedHash 期望的Hash值
   * @return 是否匹配
   */
  public static boolean verifyHash(final MultipartFile file, final String expectedHash) {
    if (file == null || expectedHash == null) {
      return false;
    }
    String actualHash = calculateHash(file);
    return expectedHash.equalsIgnoreCase(actualHash);
  }

  private static String bytesToHex(final byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
