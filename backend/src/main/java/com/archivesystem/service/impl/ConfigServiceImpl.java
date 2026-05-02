package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.RuntimeSecretProvider;
import com.archivesystem.security.SecretCryptoService;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private static final String REDACTED_VALUE = "******";

    /** SMTP 密码写入 sys_config 前加密；API 返回脱敏 */
    public static final String MAIL_SMTP_PASSWORD_KEY = "system.mail.smtp.password";
    private static final Set<String> PROTECTED_SYSTEM_KEYS = Set.of(
            RuntimeSecretProvider.KEY_JWT_SECRET,
            RuntimeSecretProvider.KEY_CRYPTO_SECRET,
            RuntimeSecretProvider.KEY_CRYPTO_LEGACY_SECRET
    );

    private final SysConfigMapper configMapper;
    private final SecretCryptoService secretCryptoService;

    // 配置缓存
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    @Override
    public Map<String, List<SysConfig>> getAllGrouped() {
        List<SysConfig> configs = configMapper.selectAllOrdered();
        return configs.stream()
                .map(this::sanitizeConfig)
                .collect(Collectors.groupingBy(
                        c -> c.getConfigGroup() != null ? c.getConfigGroup() : "OTHER"
                ));
    }

    @Override
    public List<SysConfig> getAll() {
        return configMapper.selectAllOrdered().stream()
                .map(this::sanitizeConfig)
                .toList();
    }

    @Override
    public List<SysConfig> getByGroup(String group) {
        return configMapper.selectByGroup(group).stream()
                .map(this::sanitizeConfig)
                .toList();
    }

    @Override
    public SysConfig getByKey(String key) {
        SysConfig config = configMapper.selectByKey(key);
        if (config == null) {
            throw NotFoundException.of("配置", key);
        }
        return sanitizeConfig(config);
    }

    @Override
    public String getValue(String key) {
        return configCache.get(key);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        String value = configCache.get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public Integer getIntValue(String key, Integer defaultValue) {
        String value = configCache.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值解析失败: key={}, value={}", key, value);
            return defaultValue;
        }
    }

    @Override
    public Boolean getBooleanValue(String key, Boolean defaultValue) {
        String value = configCache.get(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    @Override
    @Transactional
    public void updateConfig(String key, String value) {
        assertMutableKey(key);
        SysConfig config = configMapper.selectByKey(key);
        if (config == null) {
            throw NotFoundException.of("配置", key);
        }
        if (!Boolean.TRUE.equals(config.getEditable())) {
            throw new BusinessException("该配置项不可编辑");
        }

        if (isRedactedSensitivePlaceholder(key, value)) {
            return;
        }
        value = validateAndNormalizeByType(config.getConfigType(), key, value);
        value = normalizeStoredValue(key, value);

        config.setConfigValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(SecurityUtils.getCurrentUserId());
        configMapper.updateById(config);

        // ConcurrentHashMap 不接受 null；配置被清空时同步移除缓存项
        if (value != null) {
            configCache.put(key, value);
        } else {
            configCache.remove(key);
        }
        log.info("配置更新: key={}, value={}", key, maskValueForLog(key, value));
    }

    @Override
    @Transactional
    public void batchUpdateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            try {
                updateConfig(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("批量更新配置失败: key={}", entry.getKey(), e);
                throw e;
            }
        }
    }

    @Override
    @Transactional
    public void saveConfig(String key, String value, String group, String description, String type, Boolean editable, Integer sortOrder) {
        assertMutableKey(key);
        String targetType = type != null ? type : SysConfig.TYPE_STRING;
        value = validateAndNormalizeByType(targetType, key, value);
        value = normalizeStoredValue(key, value);
        SysConfig existing = configMapper.selectByKey(key);
        if (existing != null) {
            existing.setConfigValue(value);
            if (group != null) {
                existing.setConfigGroup(group);
            }
            if (description != null) {
                existing.setDescription(description);
            }
            if (type != null) {
                existing.setConfigType(targetType);
            }
            if (editable != null) {
                existing.setEditable(editable);
            }
            if (sortOrder != null) {
                existing.setSortOrder(sortOrder);
            }
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setUpdatedBy(SecurityUtils.getCurrentUserId());
            configMapper.updateById(existing);
        } else {
            SysConfig config = SysConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .configGroup(group)
                    .description(description)
                    .configType(targetType)
                    .editable(editable != null ? editable : true)
                    .sortOrder(sortOrder != null ? sortOrder : 0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createdBy(SecurityUtils.getCurrentUserId())
                    .updatedBy(SecurityUtils.getCurrentUserId())
                    .build();
            configMapper.insert(config);
        }

        if (value != null) {
            configCache.put(key, value);
        } else {
            configCache.remove(key);
        }
        log.info("配置保存: key={}, value={}", key, maskValueForLog(key, value));
    }

    @Override
    @Transactional
    public SysConfig createConfig(SysConfig config) {
        assertMutableKey(config != null ? config.getConfigKey() : null);
        // 检查是否已存在
        SysConfig existing = configMapper.selectByKey(config.getConfigKey());
        if (existing != null) {
            throw new BusinessException("配置键已存在: " + config.getConfigKey());
        }

        String targetType = config.getConfigType() != null ? config.getConfigType() : SysConfig.TYPE_STRING;
        config.setConfigType(targetType);
        config.setConfigValue(validateAndNormalizeByType(targetType, config.getConfigKey(), config.getConfigValue()));
        config.setConfigValue(normalizeStoredValue(config.getConfigKey(), config.getConfigValue()));
        config.setCreatedAt(LocalDateTime.now());
        config.setCreatedBy(SecurityUtils.getCurrentUserId());
        configMapper.insert(config);

        // 更新缓存
        if (config.getConfigValue() != null) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }

        return sanitizeConfig(config);
    }

    private String normalizeStoredValue(String key, String value) {
        if (!MAIL_SMTP_PASSWORD_KEY.equals(key)) {
            return value;
        }
        if (value == null || value.isBlank()) {
            return null;
        }
        return secretCryptoService.encrypt(value.trim());
    }

    private boolean isRedactedSensitivePlaceholder(String key, String value) {
        return isSensitiveKey(key) && value != null && REDACTED_VALUE.equals(value.trim());
    }

    private String validateAndNormalizeByType(String configType, String key, String value) {
        return ConfigValueValidator.validateAndNormalizeByType(configType, key, value);
    }

    @Override
    @Transactional
    public void deleteConfig(String key) {
        assertMutableKey(key);
        SysConfig config = configMapper.selectByKey(key);
        if (config == null) {
            throw NotFoundException.of("配置", key);
        }
        if (!Boolean.TRUE.equals(config.getEditable())) {
            throw new BusinessException("该配置项不可删除");
        }

        configMapper.deleteById(config.getId());
        configCache.remove(key);
        log.info("配置删除: key={}", key);
    }

    private void assertMutableKey(String key) {
        if (key == null) {
            return;
        }
        if (PROTECTED_SYSTEM_KEYS.contains(key)) {
            throw new BusinessException("该配置项由系统托管，不能通过管理接口创建、修改或删除");
        }
    }

    @Override
    public void refreshCache() {
        log.info("刷新配置缓存...");
        configCache.clear();
        List<SysConfig> configs = configMapper.selectAllOrdered();
        for (SysConfig config : configs) {
            if (config.getConfigValue() != null) {
                configCache.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        log.info("配置缓存刷新完成: {}条", configs.size());
    }

    @Override
    public String getArchiveNoPrefix(String archiveType) {
        String key = "archive.no.prefix." + archiveType;
        String prefix = getValue(key);
        if (prefix == null || prefix.isBlank()) {
            prefix = getValue("archive.no.prefix.DEFAULT", "ARC");
        }
        return prefix;
    }

    private SysConfig sanitizeConfig(SysConfig config) {
        if (config == null) {
            return null;
        }
        SysConfig sanitized = new SysConfig();
        sanitized.setId(config.getId());
        sanitized.setConfigKey(config.getConfigKey());
        sanitized.setConfigValue(isSensitiveKey(config.getConfigKey()) ? REDACTED_VALUE : config.getConfigValue());
        sanitized.setConfigType(config.getConfigType());
        sanitized.setConfigGroup(config.getConfigGroup());
        sanitized.setDescription(config.getDescription());
        sanitized.setEditable(config.getEditable());
        sanitized.setSortOrder(config.getSortOrder());
        sanitized.setCreatedAt(config.getCreatedAt());
        sanitized.setUpdatedAt(config.getUpdatedAt());
        sanitized.setCreatedBy(config.getCreatedBy());
        sanitized.setUpdatedBy(config.getUpdatedBy());
        return sanitized;
    }

    private String maskValueForLog(String key, String value) {
        return isSensitiveKey(key) ? REDACTED_VALUE : value;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return false;
        }

        List<String> markers = new ArrayList<>(List.of(
                "password",
                "secret",
                "token",
                "credential",
                "private-key",
                "private_key",
                "api-key",
                "api_key",
                "access-key",
                "access_key"
        ));
        if ("system.site.logo.object".equals(normalized)) {
            return true;
        }
        if (MAIL_SMTP_PASSWORD_KEY.equalsIgnoreCase(normalized)) {
            return true;
        }
        return markers.stream().anyMatch(normalized::contains);
    }
}
