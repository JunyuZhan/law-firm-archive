package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.UpdateConfigCommand;
import com.lawfirm.application.system.dto.SysConfigDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.SysConfig;
import com.lawfirm.domain.system.repository.SysConfigRepository;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 系统配置应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigAppService {

  /** 系统配置仓储 */
  private final SysConfigRepository configRepository;

  /** 系统配置Mapper */
  private final SysConfigMapper configMapper;

  /** 业务缓存服务 */
  private final BusinessCacheService businessCacheService;

  /**
   * 获取所有配置
   *
   * @return 配置列表
   */
  public List<SysConfigDTO> listConfigs() {
    return configMapper.selectAllConfigs().stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 根据前缀获取配置
   *
   * @param keyPrefix 键前缀
   * @return 配置列表
   */
  public List<SysConfigDTO> listConfigsByPrefix(final String keyPrefix) {
    return configMapper.selectAllConfigs().stream()
        .filter(c -> c.getConfigKey() != null && c.getConfigKey().startsWith(keyPrefix))
        .map(this::toDTO)
        .collect(Collectors.toList());
  }

  /**
   * 根据键获取配置值（带缓存）
   *
   * @param key 配置键
   * @return 配置值
   */
  public String getConfigValue(final String key) {
    return businessCacheService.getConfig(
        key,
        () -> {
          SysConfig config = configMapper.selectByKey(key);
          return config != null ? config.getConfigValue() : null;
        });
  }

  /**
   * 根据键获取配置
   *
   * @param key 配置键
   * @return 配置DTO
   */
  public SysConfigDTO getConfigByKey(final String key) {
    SysConfig config = configMapper.selectByKey(key);
    return config != null ? toDTO(config) : null;
  }

  /**
   * 批量获取配置
   *
   * @param keys 配置键列表
   * @return 配置映射
   */
  public Map<String, String> getConfigMap(final List<String> keys) {
    return configMapper.selectAllConfigs().stream()
        .filter(c -> keys.contains(c.getConfigKey()))
        .collect(Collectors.toMap(SysConfig::getConfigKey, SysConfig::getConfigValue));
  }

  /**
   * 更新配置
   *
   * @param command 更新命令
   */
  @Transactional
  public void updateConfig(final UpdateConfigCommand command) {
    SysConfig config = configRepository.getByIdOrThrow(command.getId(), "配置不存在");

    log.debug(
        "更新配置请求: id={}, configValue={}, configName={}, description={}",
        command.getId(),
        command.getConfigValue(),
        command.getConfigName(),
        command.getDescription());

    // 允许更新为空字符串，但不允许更新为null（null表示未传递该字段）
    if (command.getConfigValue() != null) {
      config.setConfigValue(command.getConfigValue());
      log.debug("设置配置值: {} = {}", config.getConfigKey(), command.getConfigValue());
    }
    if (command.getConfigName() != null) {
      config.setConfigName(command.getConfigName());
    }
    if (command.getDescription() != null) {
      config.setDescription(command.getDescription());
    }

    configRepository.updateById(config);

    // 重新查询确认更新结果
    SysConfig updated = configRepository.getByIdOrThrow(command.getId(), "配置不存在");
    log.info(
        "配置更新成功: {} = {} (更新后查询值: {})",
        config.getConfigKey(),
        config.getConfigValue(),
        updated.getConfigValue());

    // 清除所有配置缓存（包括单个key缓存和批量查询可能存在的缓存）
    businessCacheService.evictAllConfigs();
  }

  /**
   * 根据键更新配置值（如果配置不存在则自动创建）
   *
   * @param key 配置键
   * @param value 配置值
   */
  @Transactional
  public void updateConfigByKey(final String key, final String value) {
    SysConfig config = configMapper.selectByKey(key);
    if (config == null) {
      // 配置不存在，自动创建
      config =
          SysConfig.builder()
              .configKey(key)
              .configValue(value)
              .configName(getDefaultConfigName(key))
              .configType(SysConfig.TYPE_STRING)
              .description(getDefaultConfigDescription(key))
              .isSystem(false)
              .build();
      configRepository.save(config);
      log.info("配置自动创建成功: {} = {}", key, value);
    } else {
      // 配置存在，检查是否为系统内置配置
      if (Boolean.TRUE.equals(config.getIsSystem())) {
        throw new BusinessException("系统内置配置不允许修改");
      }
      configMapper.updateValueByKey(key, value);
      log.info("配置更新成功: {} = {}", key, value);
    }
    // 清除所有配置缓存
    businessCacheService.evictAllConfigs();
  }

  /**
   * 获取默认配置名称
   *
   * @param key 配置键
   * @return 默认名称
   */
  private String getDefaultConfigName(final String key) {
    if ("sys.maintenance.enabled".equals(key)) {
      return "维护模式开关";
    } else if ("sys.maintenance.message".equals(key)) {
      return "维护提示信息";
    }
    return key;
  }

  /**
   * 获取默认配置描述
   *
   * @param key 配置键
   * @return 默认描述
   */
  private String getDefaultConfigDescription(final String key) {
    if ("sys.maintenance.enabled".equals(key)) {
      return "系统维护模式开关，true表示开启维护模式，false表示关闭";
    } else if ("sys.maintenance.message".equals(key)) {
      return "系统维护模式下显示给用户的提示信息";
    }
    return null;
  }

  /**
   * 创建配置
   *
   * @param command 创建命令
   * @return 配置DTO
   */
  @Transactional
  public SysConfigDTO createConfig(final UpdateConfigCommand command) {
    // 检查键是否已存在
    if (configMapper.selectByKey(command.getConfigKey()) != null) {
      throw new BusinessException("配置键已存在");
    }

    SysConfig config =
        SysConfig.builder()
            .configKey(command.getConfigKey())
            .configValue(command.getConfigValue())
            .configName(command.getConfigName())
            .configType(
                command.getConfigType() != null ? command.getConfigType() : SysConfig.TYPE_STRING)
            .description(command.getDescription())
            .isSystem(false)
            .build();

    configRepository.save(config);
    log.info("配置创建成功: {}", config.getConfigKey());
    return toDTO(config);
  }

  /**
   * 删除配置
   *
   * @param id 配置ID
   */
  @Transactional
  public void deleteConfig(final Long id) {
    SysConfig config = configRepository.getByIdOrThrow(id, "配置不存在");
    if (Boolean.TRUE.equals(config.getIsSystem())) {
      throw new BusinessException("系统内置配置不允许删除");
    }
    configMapper.deleteById(id);
    log.info("配置删除成功: {}", config.getConfigKey());
  }

  /**
   * 转换为DTO
   *
   * @param config 配置实体
   * @return 配置DTO
   */
  /**
   * 检查配置键是否为敏感配置
   */
  private boolean isSensitiveConfig(final String configKey) {
    if (configKey == null) {
      return false;
    }
    String lowerKey = configKey.toLowerCase();
    return lowerKey.contains("password")
        || lowerKey.contains("secret")
        || lowerKey.contains("token")
        || lowerKey.contains("key")
        || lowerKey.contains("credential")
        || lowerKey.contains("apikey");
  }

  private SysConfigDTO toDTO(final SysConfig config) {
    SysConfigDTO dto = new SysConfigDTO();
    dto.setId(config.getId());
    dto.setConfigKey(config.getConfigKey());
    // 敏感配置值脱敏处理
    if (isSensitiveConfig(config.getConfigKey())) {
      dto.setConfigValue("******");
    } else {
      dto.setConfigValue(config.getConfigValue());
    }
    dto.setConfigName(config.getConfigName());
    dto.setConfigType(config.getConfigType());
    dto.setDescription(config.getDescription());
    dto.setIsSystem(config.getIsSystem());
    dto.setCreatedAt(config.getCreatedAt());
    dto.setUpdatedAt(config.getUpdatedAt());
    return dto;
  }
}
