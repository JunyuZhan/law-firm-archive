package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.UpdateConfigCommand;
import com.lawfirm.application.system.dto.SysConfigDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.SysConfig;
import com.lawfirm.domain.system.repository.SysConfigRepository;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigAppService {

    private final SysConfigRepository configRepository;
    private final SysConfigMapper configMapper;

    /**
     * 获取所有配置
     */
    public List<SysConfigDTO> listConfigs() {
        return configMapper.selectAllConfigs().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据键获取配置值
     */
    public String getConfigValue(String key) {
        SysConfig config = configMapper.selectByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 根据键获取配置
     */
    public SysConfigDTO getConfigByKey(String key) {
        SysConfig config = configMapper.selectByKey(key);
        return config != null ? toDTO(config) : null;
    }

    /**
     * 批量获取配置
     */
    public Map<String, String> getConfigMap(List<String> keys) {
        return configMapper.selectAllConfigs().stream()
                .filter(c -> keys.contains(c.getConfigKey()))
                .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
    }

    /**
     * 更新配置
     */
    @Transactional
    public void updateConfig(UpdateConfigCommand command) {
        SysConfig config = configRepository.getByIdOrThrow(command.getId(), "配置不存在");
        
        if (StringUtils.hasText(command.getConfigValue())) {
            config.setConfigValue(command.getConfigValue());
        }
        if (StringUtils.hasText(command.getConfigName())) {
            config.setConfigName(command.getConfigName());
        }
        if (StringUtils.hasText(command.getDescription())) {
            config.setDescription(command.getDescription());
        }
        
        configRepository.updateById(config);
        log.info("配置更新成功: {}", config.getConfigKey());
    }

    /**
     * 根据键更新配置值
     */
    @Transactional
    public void updateConfigByKey(String key, String value) {
        SysConfig config = configMapper.selectByKey(key);
        if (config == null) {
            throw new BusinessException("配置不存在: " + key);
        }
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new BusinessException("系统内置配置不允许修改");
        }
        configMapper.updateValueByKey(key, value);
        log.info("配置更新成功: {} = {}", key, value);
    }

    /**
     * 创建配置
     */
    @Transactional
    public SysConfigDTO createConfig(UpdateConfigCommand command) {
        // 检查键是否已存在
        if (configMapper.selectByKey(command.getConfigKey()) != null) {
            throw new BusinessException("配置键已存在");
        }
        
        SysConfig config = SysConfig.builder()
                .configKey(command.getConfigKey())
                .configValue(command.getConfigValue())
                .configName(command.getConfigName())
                .configType(command.getConfigType() != null ? command.getConfigType() : SysConfig.TYPE_STRING)
                .description(command.getDescription())
                .isSystem(false)
                .build();
        
        configRepository.save(config);
        log.info("配置创建成功: {}", config.getConfigKey());
        return toDTO(config);
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(Long id) {
        SysConfig config = configRepository.getByIdOrThrow(id, "配置不存在");
        if (Boolean.TRUE.equals(config.getIsSystem())) {
            throw new BusinessException("系统内置配置不允许删除");
        }
        configMapper.deleteById(id);
        log.info("配置删除成功: {}", config.getConfigKey());
    }

    private SysConfigDTO toDTO(SysConfig config) {
        SysConfigDTO dto = new SysConfigDTO();
        dto.setId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setConfigName(config.getConfigName());
        dto.setConfigType(config.getConfigType());
        dto.setDescription(config.getDescription());
        dto.setIsSystem(config.getIsSystem());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }
}
