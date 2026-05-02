package com.archivesystem.security;

import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 运行时密钥提供器：优先读取数据库，空库时自动生成并持久化。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuntimeSecretProvider {

    public static final String KEY_JWT_SECRET = "system.security.jwt.secret";
    public static final String KEY_CRYPTO_SECRET = "system.security.crypto.secret";
    public static final String KEY_CRYPTO_LEGACY_SECRET = "system.security.crypto.legacy-secret";

    private static final int SECRET_BYTES = 48;

    private final SysConfigMapper sysConfigMapper;

    @Value("${jwt.secret:}")
    private String fallbackJwtSecret;

    @Value("${security.crypto.secret:}")
    private String fallbackCryptoSecret;

    @Value("${security.crypto.legacy-secret:}")
    private String fallbackLegacyCryptoSecret;

    private final SecureRandom secureRandom = new SecureRandom();

    private volatile String jwtSecret;
    private volatile String cryptoSecret;
    private volatile String legacyCryptoSecret;

    public String getJwtSecret() {
        String cached = jwtSecret;
        if (hasText(cached)) {
            return cached;
        }
        synchronized (this) {
            if (!hasText(jwtSecret)) {
                jwtSecret = loadOrCreateSecret(KEY_JWT_SECRET, fallbackJwtSecret, "JWT");
            }
            return jwtSecret;
        }
    }

    public String getCryptoSecret() {
        String cached = cryptoSecret;
        if (hasText(cached)) {
            return cached;
        }
        synchronized (this) {
            if (!hasText(cryptoSecret)) {
                cryptoSecret = loadOrCreateSecret(KEY_CRYPTO_SECRET, fallbackCryptoSecret, "Crypto");
            }
            return cryptoSecret;
        }
    }

    public String getLegacyCryptoSecret() {
        String cached = legacyCryptoSecret;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (legacyCryptoSecret == null) {
                legacyCryptoSecret = loadOptionalSecret(KEY_CRYPTO_LEGACY_SECRET, fallbackLegacyCryptoSecret);
            }
            return legacyCryptoSecret;
        }
    }

    private String loadOrCreateSecret(String key, String fallbackValue, String label) {
        SysConfig existing = sysConfigMapper.selectByKey(key);
        if (existing != null && hasText(existing.getConfigValue())) {
            String secret = existing.getConfigValue().trim();
            upsertSecretConfig(key, secret);
            return secret;
        }

        String secret = hasText(fallbackValue) ? fallbackValue.trim() : generateSecret();
        upsertSecretConfig(key, secret);
        log.info("{} 密钥已自动初始化并持久化到 sys_config", label);
        return secret;
    }

    private String loadOptionalSecret(String key, String fallbackValue) {
        SysConfig existing = sysConfigMapper.selectByKey(key);
        if (existing != null && hasText(existing.getConfigValue())) {
            String secret = existing.getConfigValue().trim();
            upsertSecretConfig(key, secret);
            return secret;
        }
        if (!hasText(fallbackValue)) {
            return null;
        }
        String secret = fallbackValue.trim();
        upsertSecretConfig(key, secret);
        return secret;
    }

    private void upsertSecretConfig(String key, String value) {
        SysConfig existing = sysConfigMapper.selectByKey(key);
        if (existing == null) {
            SysConfig config = SysConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .configType(SysConfig.TYPE_STRING)
                    .configGroup(SysConfig.GROUP_SYSTEM)
                    .description(describeKey(key))
                    .editable(false)
                    .sortOrder(sortOrder(key))
                    .build();
            sysConfigMapper.insert(config);
            return;
        }
        existing.setConfigValue(value);
        existing.setEditable(false);
        if (!hasText(existing.getDescription())) {
            existing.setDescription(describeKey(key));
        }
        if (existing.getSortOrder() == null || existing.getSortOrder() == 0) {
            existing.setSortOrder(sortOrder(key));
        }
        sysConfigMapper.updateById(existing);
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String describeKey(String key) {
        return switch (key) {
            case KEY_JWT_SECRET -> "运行时自动生成的 JWT 签名密钥";
            case KEY_CRYPTO_SECRET -> "运行时自动生成的敏感配置加密密钥";
            case KEY_CRYPTO_LEGACY_SECRET -> "兼容旧环境的敏感配置加密密钥";
            default -> "运行时自动生成的系统密钥";
        };
    }

    private int sortOrder(String key) {
        return switch (key) {
            case KEY_JWT_SECRET -> 90;
            case KEY_CRYPTO_SECRET -> 91;
            case KEY_CRYPTO_LEGACY_SECRET -> 92;
            default -> 99;
        };
    }
}
