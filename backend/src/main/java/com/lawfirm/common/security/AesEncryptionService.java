package com.lawfirm.common.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * AES加密服务实现
 *
 * <p>使用 AES/CBC/PKCS5Padding 算法
 *
 * <p>配置项（application.yml）： lawfirm: security: aes-key: "your-32-char-key-here-12345678" #
 * 32字节密钥（任意长度会自动派生为32字节） aes-iv: "your-16-char-iv!" # 16字节IV（任意长度会自动派生为16字节） sign-key:
 * "your-sign-secret-key" # 签名密钥
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
@Slf4j
@Service
public class AesEncryptionService implements EncryptionService {

  /** AES算法 */
  private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

  /** HMAC算法 */
  private static final String HMAC_ALGORITHM = "HmacSHA256";

  /** Hash算法 */
  private static final String HASH_ALGORITHM = "SHA-256";

  /** AES密钥大小（AES-256） */
  private static final int AES_KEY_SIZE = 32;

  /** AES IV大小 */
  private static final int AES_IV_SIZE = 16;

  /** AES密钥配置 */
  @Value("${lawfirm.security.aes-key:LawFirmSecretKey1234567890123456}")
  private String aesKeyConfig;

  /** AES IV配置 */
  @Value("${lawfirm.security.aes-iv:LawFirmSecretIV!}")
  private String aesIvConfig;

  /** 派生后的实际密钥 */
  private byte[] derivedKey;

  /** 派生后的实际IV */
  private byte[] derivedIv;

  /** 初始化加密服务 */
  @PostConstruct
  public void init() {
    // 使用SHA-256派生密钥，确保长度正确
    derivedKey = deriveKey(aesKeyConfig, AES_KEY_SIZE);
    derivedIv = deriveKey(aesIvConfig, AES_IV_SIZE);
    log.info("AES加密服务初始化完成，密钥长度: {}字节, IV长度: {}字节", derivedKey.length, derivedIv.length);
  }

  /**
   * 使用SHA-256派生指定长度的密钥
   *
   * @param input 输入字符串
   * @param length 目标长度
   * @return 派生后的密钥
   */
  private byte[] deriveKey(final String input, final int length) {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return Arrays.copyOf(hash, length);
    } catch (Exception e) {
      log.error("密钥派生失败", e);
      throw new SecurityException("密钥派生失败: " + e.getMessage());
    }
  }

  /** 签名密钥配置 */
  @Value("${lawfirm.security.sign-key:LawFirmSignatureSecretKey}")
  private String signKey;

  /**
   * 加密明文
   *
   * @param plaintext 明文
   * @return 密文
   */
  @Override
  public String encrypt(final String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) {
      return plaintext;
    }
    try {
      SecretKeySpec keySpec = new SecretKeySpec(derivedKey, "AES");
      IvParameterSpec ivSpec = new IvParameterSpec(derivedIv);

      Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      log.error("加密失败", e);
      throw new SecurityException("加密失败: " + e.getMessage());
    }
  }

  /**
   * 解密密文
   *
   * @param ciphertext 密文
   * @return 明文
   */
  @Override
  public String decrypt(final String ciphertext) {
    if (ciphertext == null || ciphertext.isEmpty()) {
      return ciphertext;
    }
    try {
      SecretKeySpec keySpec = new SecretKeySpec(derivedKey, "AES");
      IvParameterSpec ivSpec = new IvParameterSpec(derivedIv);

      Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

      byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      log.error("解密失败", e);
      throw new SecurityException("解密失败: " + e.getMessage());
    }
  }

  /**
   * 签名数据
   *
   * @param data 数据
   * @return 签名
   */
  @Override
  public String sign(final String data) {
    if (data == null) {
      return null;
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      SecretKeySpec keySpec =
          new SecretKeySpec(signKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
      mac.init(keySpec);

      byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(signature);
    } catch (Exception e) {
      log.error("签名失败", e);
      throw new SecurityException("签名失败: " + e.getMessage());
    }
  }

  /**
   * 验证签名
   *
   * @param data 数据
   * @param signature 签名
   * @return 是否有效
   */
  @Override
  public boolean verify(final String data, final String signature) {
    if (data == null || signature == null) {
      return false;
    }
    String expectedSignature = sign(data);
    return signature.equalsIgnoreCase(expectedSignature);
  }

  /**
   * 计算哈希值
   *
   * @param data 数据
   * @return 哈希值
   */
  @Override
  public String hash(final String data) {
    if (data == null) {
      return null;
    }
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
      byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (Exception e) {
      log.error("哈希失败", e);
      throw new SecurityException("哈希失败: " + e.getMessage());
    }
  }

  /**
   * 字节数组转十六进制字符串
   *
   * @param bytes 字节数组
   * @return 十六进制字符串
   */
  private String bytesToHex(final byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /**
   * 加密敏感字段（用于身份证号、银行卡号等） 如果加密失败，返回原文（降级处理）
   *
   * @param value 敏感值
   * @return 加密后的值或原文
   */
  public String encryptSensitive(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    try {
      return encrypt(value);
    } catch (Exception e) {
      log.warn("敏感字段加密失败，返回原文", e);
      return value;
    }
  }

  /**
   * 解密敏感字段 如果解密失败，返回原文（可能是未加密的历史数据）
   *
   * @param value 加密值
   * @return 解密后的值或原文
   */
  public String decryptSensitive(final String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    try {
      return decrypt(value);
    } catch (Exception e) {
      log.debug("敏感字段解密失败，可能是未加密数据，返回原文");
      return value;
    }
  }
}
