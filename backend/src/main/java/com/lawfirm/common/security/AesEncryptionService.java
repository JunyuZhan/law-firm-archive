package com.lawfirm.common.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES加密服务实现
 * 
 * 使用 AES/CBC/PKCS5Padding 算法
 * 
 * 配置项（application.yml）：
 * lawfirm:
 *   security:
 *     aes-key: "your-32-char-key-here-12345678"  # 32字节密钥（任意长度会自动派生为32字节）
 *     aes-iv: "your-16-char-iv!"                 # 16字节IV（任意长度会自动派生为16字节）
 *     sign-key: "your-sign-secret-key"           # 签名密钥
 * 
 * @author Kiro-1
 * @since 2026-01-10
 */
@Slf4j
@Service
public class AesEncryptionService implements EncryptionService {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int AES_KEY_SIZE = 32; // AES-256
    private static final int AES_IV_SIZE = 16;

    @Value("${lawfirm.security.aes-key:LawFirmSecretKey1234567890123456}")
    private String aesKeyConfig;

    @Value("${lawfirm.security.aes-iv:LawFirmSecretIV!}")
    private String aesIvConfig;

    // 派生后的实际密钥
    private byte[] derivedKey;
    private byte[] derivedIv;

    @PostConstruct
    public void init() {
        // 使用SHA-256派生密钥，确保长度正确
        derivedKey = deriveKey(aesKeyConfig, AES_KEY_SIZE);
        derivedIv = deriveKey(aesIvConfig, AES_IV_SIZE);
        log.info("AES加密服务初始化完成，密钥长度: {}字节, IV长度: {}字节", derivedKey.length, derivedIv.length);
    }

    /**
     * 使用SHA-256派生指定长度的密钥
     */
    private byte[] deriveKey(String input, int length) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, length);
        } catch (Exception e) {
            log.error("密钥派生失败", e);
            throw new SecurityException("密钥派生失败: " + e.getMessage());
        }
    }

    @Value("${lawfirm.security.sign-key:LawFirmSignatureSecretKey}")
    private String signKey;

    @Override
    public String encrypt(String plaintext) {
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

    @Override
    public String decrypt(String ciphertext) {
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

    @Override
    public String sign(String data) {
        if (data == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    signKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(signature);
        } catch (Exception e) {
            log.error("签名失败", e);
            throw new SecurityException("签名失败: " + e.getMessage());
        }
    }

    @Override
    public boolean verify(String data, String signature) {
        if (data == null || signature == null) {
            return false;
        }
        String expectedSignature = sign(data);
        return signature.equalsIgnoreCase(expectedSignature);
    }

    @Override
    public String hash(String data) {
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
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 加密敏感字段（用于身份证号、银行卡号等）
     * 如果加密失败，返回原文（降级处理）
     */
    public String encryptSensitive(String value) {
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
     * 解密敏感字段
     * 如果解密失败，返回原文（可能是未加密的历史数据）
     */
    public String decryptSensitive(String value) {
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

