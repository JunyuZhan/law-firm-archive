package com.lawfirm.common.security;

/**
 * 加密服务接口
 *
 * <p>提供敏感数据加密、解密、签名等功能
 *
 * @author junyuzhan
 * @since 2026-01-10
 */
public interface EncryptionService {

  /**
   * 加密（AES）
   *
   * @param plaintext 明文
   * @return 密文（Base64编码）
   */
  String encrypt(String plaintext);

  /**
   * 解密（AES）
   *
   * @param ciphertext 密文（Base64编码）
   * @return 明文
   */
  String decrypt(String ciphertext);

  /**
   * 生成数据签名
   *
   * @param data 待签名数据
   * @return 签名（Hex编码）
   */
  String sign(String data);

  /**
   * 验证数据签名
   *
   * @param data 原始数据
   * @param signature 签名
   * @return 是否有效
   */
  boolean verify(String data, String signature);

  /**
   * 哈希（用于不可逆场景）
   *
   * @param data 原始数据
   * @return 哈希值（Hex编码）
   */
  String hash(String data);
}
