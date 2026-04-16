package com.archivesystem.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * 敏感配置加解密服务.
 * @author junyuzhan
 */
@Slf4j
@Component
public class SecretCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKeySpec keySpec;
    private final SecretKeySpec legacyKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretCryptoService(
            @Value("${security.crypto.secret:}") String secret,
            @Value("${security.crypto.legacy-secret:${jwt.secret:}}") String legacySecret) {
        this(validateSecret(secret), normalizeOptionalSecret(legacySecret), true);
    }

    SecretCryptoService(String secret) {
        this(validateSecret(secret), null, true);
    }

    SecretCryptoService(String secret, String legacySecret, boolean normalizeSecrets, int ignored) {
        this(
                normalizeSecrets ? validateSecret(secret) : secret,
                normalizeSecrets ? normalizeOptionalSecret(legacySecret) : legacySecret,
                true
        );
    }

    private SecretCryptoService(String secret, String legacySecret, boolean normalized) {
        this.keySpec = new SecretKeySpec(deriveKey(secret), "AES");
        this.legacyKeySpec = Optional.ofNullable(normalizeOptionalSecret(legacySecret))
                .filter(candidate -> !candidate.equals(secret))
                .map(this::deriveKey)
                .map(key -> new SecretKeySpec(key, "AES"))
                .orElse(null);
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("敏感配置加密失败", e);
            throw new IllegalStateException("敏感配置加密失败");
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        try {
            return decryptWithKey(ciphertext, keySpec);
        } catch (Exception e) {
            if (legacyKeySpec != null) {
                try {
                    log.info("敏感配置使用兼容旧密钥解密，建议尽快重新保存配置以完成迁移");
                    return decryptWithKey(ciphertext, legacyKeySpec);
                } catch (Exception legacyException) {
                    log.error("敏感配置解密失败", legacyException);
                    throw new IllegalStateException("敏感配置解密失败");
                }
            }
            log.error("敏感配置解密失败", e);
            throw new IllegalStateException("敏感配置解密失败");
        }
    }

    private byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化敏感配置加密密钥", e);
        }
    }

    private String decryptWithKey(String ciphertext, SecretKeySpec secretKeySpec) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(ciphertext);
        ByteBuffer buffer = ByteBuffer.wrap(decoded);
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);
        byte[] encrypted = new byte[buffer.remaining()];
        buffer.get(encrypted);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static String validateSecret(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("security.crypto.secret 未配置，服务启动已拒绝");
        }
        String normalized = rawSecret.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("security.crypto.secret 长度不足，至少需要32字节");
        }
        return normalized;
    }

    private static String normalizeOptionalSecret(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            return null;
        }
        String normalized = rawSecret.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            return null;
        }
        return normalized;
    }
}
