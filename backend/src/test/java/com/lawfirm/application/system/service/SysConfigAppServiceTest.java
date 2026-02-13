package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.lawfirm.application.system.command.UpdateConfigCommand;
import com.lawfirm.application.system.dto.SysConfigDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.SysConfig;
import com.lawfirm.domain.system.repository.SysConfigRepository;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
import com.lawfirm.infrastructure.persistence.mapper.SysConfigMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SysConfigAppService 单元测试
 *
 * <p>测试系统配置应用服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysConfigAppService 系统配置服务测试")
class SysConfigAppServiceTest {

  @Mock private SysConfigRepository configRepository;

  @Mock private SysConfigMapper configMapper;

  @Mock private BusinessCacheService businessCacheService;

  @InjectMocks private SysConfigAppService service;

  // ========== 查询测试 ==========

  @Test
  @DisplayName("应该获取所有配置")
  void listConfigs_shouldReturnAllConfigs() {
    SysConfig config1 = createTestConfig(1L, "config1", "value1", "配置1");
    SysConfig config2 = createTestConfig(2L, "config2", "value2", "配置2");

    when(configMapper.selectAllConfigs()).thenReturn(List.of(config1, config2));

    List<SysConfigDTO> result = service.listConfigs();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getConfigKey()).isEqualTo("config1");
    assertThat(result.get(1).getConfigKey()).isEqualTo("config2");
  }

  @Test
  @DisplayName("应该根据前缀获取配置")
  void listConfigsByPrefix_shouldReturnFilteredConfigs() {
    SysConfig config1 = createTestConfig(1L, "firm.name", "测试律所", "律所名称");
    SysConfig config2 = createTestConfig(2L, "firm.address", "北京市", "律所地址");
    SysConfig config3 = createTestConfig(3L, "other.config", "value", "其他配置");

    when(configMapper.selectAllConfigs()).thenReturn(List.of(config1, config2, config3));

    List<SysConfigDTO> result = service.listConfigsByPrefix("firm.");

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(c -> c.getConfigKey().startsWith("firm."));
  }

  @Test
  @DisplayName("应该根据键获取配置值")
  void getConfigValue_shouldReturnValue() {
    SysConfig config = createTestConfig(1L, "test.config", "test.value", "测试配置");

    when(businessCacheService.getConfig(anyString(), any()))
        .thenAnswer(
            invocation -> {
              var supplier = invocation.getArgument(1);
              return (String) ((java.util.function.Supplier<?>) supplier).get();
            });
    when(configMapper.selectByKey("test.config")).thenReturn(config);

    String result = service.getConfigValue("test.config");

    assertThat(result).isEqualTo("test.value");
  }

  @Test
  @DisplayName("配置不存在时应该返回null")
  void getConfigValue_shouldReturnNullWhenNotFound() {
    when(businessCacheService.getConfig(anyString(), any()))
        .thenAnswer(
            invocation -> {
              var supplier = invocation.getArgument(1);
              return ((java.util.function.Supplier<?>) supplier).get();
            });
    when(configMapper.selectByKey("non.existent")).thenReturn(null);

    String result = service.getConfigValue("non.existent");

    assertThat(result).isNull();
  }

  @Test
  @DisplayName("应该根据键获取配置")
  void getConfigByKey_shouldReturnConfig() {
    SysConfig config = createTestConfig(1L, "test.config", "test.value", "测试配置");

    when(configMapper.selectByKey("test.config")).thenReturn(config);

    SysConfigDTO result = service.getConfigByKey("test.config");

    assertThat(result).isNotNull();
    assertThat(result.getConfigKey()).isEqualTo("test.config");
    assertThat(result.getConfigValue()).isEqualTo("test.value");
  }

  @Test
  @DisplayName("应该批量获取配置")
  void getConfigMap_shouldReturnConfigMap() {
    SysConfig config1 = createTestConfig(1L, "config1", "value1", "配置1");
    SysConfig config2 = createTestConfig(2L, "config2", "value2", "配置2");

    when(configMapper.selectAllConfigs()).thenReturn(List.of(config1, config2));

    Map<String, String> result = service.getConfigMap(List.of("config1", "config2"));

    assertThat(result).hasSize(2);
    assertThat(result.get("config1")).isEqualTo("value1");
    assertThat(result.get("config2")).isEqualTo("value2");
  }

  // ========== 更新测试 ==========

  @Test
  @DisplayName("应该成功更新配置")
  void updateConfig_shouldUpdateSuccessfully() {
    SysConfig config = createTestConfig(1L, "test.config", "old.value", "测试配置");

    when(configRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(config);
    when(configRepository.updateById(any(SysConfig.class)))
        .thenAnswer(
            invocation -> {
              config.setConfigValue("new.value");
              return true;
            });

    UpdateConfigCommand command = new UpdateConfigCommand();
    command.setId(1L);
    command.setConfigValue("new.value");

    service.updateConfig(command);

    assertThat(config.getConfigValue()).isEqualTo("new.value");
    verify(businessCacheService).evictAllConfigs();
  }

  @Test
  @DisplayName("应该只更新非null字段")
  void updateConfig_shouldOnlyUpdateNonNullFields() {
    SysConfig config = createTestConfig(1L, "test.config", "value", "测试配置");
    config.setConfigName("旧名称");
    config.setDescription("旧描述");

    when(configRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(config);
    when(configRepository.updateById(any(SysConfig.class)))
        .thenAnswer(
            invocation -> {
              config.setConfigValue("new.value");
              return true;
            });

    UpdateConfigCommand command = new UpdateConfigCommand();
    command.setId(1L);
    command.setConfigValue("new.value");
    // configName 和 description 不设置，应该保持原值

    service.updateConfig(command);

    assertThat(config.getConfigValue()).isEqualTo("new.value");
    assertThat(config.getConfigName()).isEqualTo("旧名称");
    assertThat(config.getDescription()).isEqualTo("旧描述");
  }

  @Test
  @DisplayName("应该根据键更新配置值")
  void updateConfigByKey_shouldUpdateExistingConfig() {
    SysConfig config = createTestConfig(1L, "test.config", "old.value", "测试配置");
    config.setIsSystem(false);

    when(configMapper.selectByKey("test.config")).thenReturn(config);
    when(configMapper.updateValueByKey("test.config", "new.value")).thenReturn(1);

    service.updateConfigByKey("test.config", "new.value");

    verify(configMapper).updateValueByKey("test.config", "new.value");
    verify(businessCacheService).evictAllConfigs();
  }

  @Test
  @DisplayName("系统内置配置不允许修改")
  void updateConfigByKey_shouldThrowExceptionForSystemConfig() {
    SysConfig config = createTestConfig(1L, "sys.config", "value", "系统配置");
    config.setIsSystem(true);

    when(configMapper.selectByKey("sys.config")).thenReturn(config);

    assertThatThrownBy(() -> service.updateConfigByKey("sys.config", "new.value"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("系统内置配置不允许修改");
  }

  @Test
  @DisplayName("配置不存在时应该自动创建")
  void updateConfigByKey_shouldCreateWhenNotExists() {
    when(configMapper.selectByKey("new.config")).thenReturn(null);
    when(configRepository.save(any(SysConfig.class)))
        .thenAnswer(
            invocation -> {
              SysConfig config = invocation.getArgument(0);
              config.setId(1L);
              return true;
            });

    service.updateConfigByKey("new.config", "new.value");

    verify(configRepository).save(any(SysConfig.class));
    verify(businessCacheService).evictAllConfigs();
  }

  // ========== 创建测试 ==========

  @Test
  @DisplayName("应该成功创建配置")
  void createConfig_shouldCreateSuccessfully() {
    when(configMapper.selectByKey("new.config")).thenReturn(null);
    when(configRepository.save(any(SysConfig.class)))
        .thenAnswer(
            invocation -> {
              SysConfig config = invocation.getArgument(0);
              config.setId(1L);
              return true;
            });

    UpdateConfigCommand command = new UpdateConfigCommand();
    command.setConfigKey("new.config");
    command.setConfigValue("new.value");
    command.setConfigName("新配置");
    command.setDescription("配置描述");

    SysConfigDTO result = service.createConfig(command);

    assertThat(result.getConfigKey()).isEqualTo("new.config");
    assertThat(result.getConfigValue()).isEqualTo("new.value");
  }

  @Test
  @DisplayName("配置键已存在时应该抛出异常")
  void createConfig_shouldThrowExceptionWhenKeyExists() {
    SysConfig existingConfig = createTestConfig(1L, "existing.config", "value", "已存在的配置");

    when(configMapper.selectByKey("existing.config")).thenReturn(existingConfig);

    UpdateConfigCommand command = new UpdateConfigCommand();
    command.setConfigKey("existing.config");
    command.setConfigValue("value");
    command.setConfigName("配置");

    assertThatThrownBy(() -> service.createConfig(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("配置键已存在");
  }

  @Test
  @DisplayName("创建配置时应该使用默认类型")
  void createConfig_shouldUseDefaultType() {
    when(configMapper.selectByKey("new.config")).thenReturn(null);
    when(configRepository.save(any(SysConfig.class)))
        .thenAnswer(
            invocation -> {
              SysConfig config = invocation.getArgument(0);
              config.setId(1L);
              return true;
            });

    UpdateConfigCommand command = new UpdateConfigCommand();
    command.setConfigKey("new.config");
    command.setConfigValue("value");
    command.setConfigName("配置");
    command.setConfigType(null); // 不设置类型，应该使用默认值

    SysConfigDTO result = service.createConfig(command);

    assertThat(result.getConfigType()).isEqualTo(SysConfig.TYPE_STRING);
  }

  // ========== 删除测试 ==========

  @Test
  @DisplayName("应该成功删除配置")
  void deleteConfig_shouldDeleteSuccessfully() {
    SysConfig config = createTestConfig(1L, "test.config", "value", "测试配置");
    config.setIsSystem(false);

    when(configRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(config);
    when(configMapper.deleteById(1L)).thenReturn(1);

    service.deleteConfig(1L);

    verify(configMapper).deleteById(1L);
  }

  @Test
  @DisplayName("系统内置配置不允许删除")
  void deleteConfig_shouldThrowExceptionForSystemConfig() {
    SysConfig config = createTestConfig(1L, "sys.config", "value", "系统配置");
    config.setIsSystem(true);

    when(configRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(config);

    assertThatThrownBy(() -> service.deleteConfig(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("系统内置配置不允许删除");

    verify(configMapper, never()).deleteById(any());
  }

  // ========== 辅助方法 ==========

  private SysConfig createTestConfig(Long id, String key, String value, String name) {
    return SysConfig.builder()
        .id(id)
        .configKey(key)
        .configValue(value)
        .configName(name)
        .configType(SysConfig.TYPE_STRING)
        .isSystem(false)
        .build();
  }
}
