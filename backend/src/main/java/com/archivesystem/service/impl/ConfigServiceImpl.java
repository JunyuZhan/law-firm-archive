package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigServiceImpl implements ConfigService {

    private final SysConfigMapper configMapper;

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
                .collect(Collectors.groupingBy(
                        c -> c.getConfigGroup() != null ? c.getConfigGroup() : "OTHER"
                ));
    }

    @Override
    public List<SysConfig> getAll() {
        return configMapper.selectAllOrdered();
    }

    @Override
    public List<SysConfig> getByGroup(String group) {
        return configMapper.selectByGroup(group);
    }

    @Override
    public SysConfig getByKey(String key) {
        SysConfig config = configMapper.selectByKey(key);
        if (config == null) {
            throw NotFoundException.of("配置", key);
        }
        return config;
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
        SysConfig config = configMapper.selectByKey(key);
        if (config == null) {
            throw NotFoundException.of("配置", key);
        }
        if (!Boolean.TRUE.equals(config.getEditable())) {
            throw new BusinessException("该配置项不可编辑");
        }

        config.setConfigValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(SecurityUtils.getCurrentUserId());
        configMapper.updateById(config);

        // 更新缓存
        configCache.put(key, value);
        log.info("配置更新: key={}, value={}", key, value);
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
    public SysConfig createConfig(SysConfig config) {
        // 检查是否已存在
        SysConfig existing = configMapper.selectByKey(config.getConfigKey());
        if (existing != null) {
            throw new BusinessException("配置键已存在: " + config.getConfigKey());
        }

        config.setCreatedAt(LocalDateTime.now());
        config.setCreatedBy(SecurityUtils.getCurrentUserId());
        configMapper.insert(config);

        // 更新缓存
        if (config.getConfigValue() != null) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }

        return config;
    }

    @Override
    @Transactional
    public void deleteConfig(String key) {
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
        if (prefix == null) {
            prefix = getValue("archive.no.prefix.DEFAULT", "ARC");
        }
        return prefix;
    }
}
