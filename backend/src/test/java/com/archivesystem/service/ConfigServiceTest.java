package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.repository.SysConfigMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.impl.ConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private SysConfigMapper configMapper;

    @InjectMocks
    private ConfigServiceImpl configService;

    private SysConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new SysConfig();
        testConfig.setId(1L);
        testConfig.setConfigKey("test.key");
        testConfig.setConfigValue("test-value");
        testConfig.setConfigGroup("SYSTEM");
        testConfig.setDescription("测试配置");
        testConfig.setEditable(true);
    }

    @Test
    void testGetAllGrouped() {
        SysConfig config1 = new SysConfig();
        config1.setConfigKey("key1");
        config1.setConfigValue("value1");
        config1.setConfigGroup("GROUP_A");

        SysConfig config2 = new SysConfig();
        config2.setConfigKey("key2");
        config2.setConfigValue("value2");
        config2.setConfigGroup("GROUP_B");

        SysConfig config3 = new SysConfig();
        config3.setConfigKey("key3");
        config3.setConfigValue("value3");
        config3.setConfigGroup(null); // 无分组

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(config1, config2, config3));

        Map<String, List<SysConfig>> result = configService.getAllGrouped();

        assertNotNull(result);
        assertTrue(result.containsKey("GROUP_A"));
        assertTrue(result.containsKey("GROUP_B"));
        assertTrue(result.containsKey("OTHER")); // null 分组归入 OTHER
    }

    @Test
    void testGetAll() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));

        List<SysConfig> result = configService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test.key", result.get(0).getConfigKey());
    }

    @Test
    void testGetByGroup() {
        when(configMapper.selectByGroup("SYSTEM")).thenReturn(Arrays.asList(testConfig));

        List<SysConfig> result = configService.getByGroup("SYSTEM");

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetByKey_Success() {
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        SysConfig result = configService.getByKey("test.key");

        assertNotNull(result);
        assertEquals("test-value", result.getConfigValue());
    }

    @Test
    void testGetByKey_NotFound() {
        when(configMapper.selectByKey("not.exists")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> configService.getByKey("not.exists"));
    }

    @Test
    void testGetValue() {
        // 首先刷新缓存
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
        configService.refreshCache();

        String result = configService.getValue("test.key");

        assertEquals("test-value", result);
    }

    @Test
    void testGetValue_NotInCache() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        String result = configService.getValue("not.in.cache");

        assertNull(result);
    }

    @Test
    void testGetValue_WithDefault() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        String result = configService.getValue("not.exists", "default-value");

        assertEquals("default-value", result);
    }

    @Test
    void testGetValue_WithDefault_ValueExists() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
        configService.refreshCache();

        String result = configService.getValue("test.key", "default-value");

        assertEquals("test-value", result);
    }

    @Test
    void testGetIntValue() {
        SysConfig intConfig = new SysConfig();
        intConfig.setConfigKey("int.key");
        intConfig.setConfigValue("42");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(intConfig));
        configService.refreshCache();

        Integer result = configService.getIntValue("int.key", 0);

        assertEquals(42, result);
    }

    @Test
    void testGetIntValue_Default() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        Integer result = configService.getIntValue("not.exists", 100);

        assertEquals(100, result);
    }

    @Test
    void testGetIntValue_InvalidFormat() {
        SysConfig invalidConfig = new SysConfig();
        invalidConfig.setConfigKey("invalid.int");
        invalidConfig.setConfigValue("not-a-number");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(invalidConfig));
        configService.refreshCache();

        Integer result = configService.getIntValue("invalid.int", 50);

        assertEquals(50, result);
    }

    @Test
    void testGetBooleanValue_True() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setConfigKey("bool.key");
        boolConfig.setConfigValue("true");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(boolConfig));
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("bool.key", false);

        assertTrue(result);
    }

    @Test
    void testGetBooleanValue_One() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setConfigKey("bool.key");
        boolConfig.setConfigValue("1");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(boolConfig));
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("bool.key", false);

        assertTrue(result);
    }

    @Test
    void testGetBooleanValue_False() {
        SysConfig boolConfig = new SysConfig();
        boolConfig.setConfigKey("bool.key");
        boolConfig.setConfigValue("false");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(boolConfig));
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("bool.key", true);

        assertFalse(result);
    }

    @Test
    void testGetBooleanValue_Default() {
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList());
        configService.refreshCache();

        Boolean result = configService.getBooleanValue("not.exists", true);

        assertTrue(result);
    }

    @Test
    void testUpdateConfig_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            when(configMapper.selectByKey("test.key")).thenReturn(testConfig);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);
            when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(testConfig));
            configService.refreshCache();

            assertDoesNotThrow(() -> configService.updateConfig("test.key", "new-value"));

            verify(configMapper).updateById(argThat(config -> 
                "new-value".equals(config.getConfigValue())));
        }
    }

    @Test
    void testUpdateConfig_NotFound() {
        when(configMapper.selectByKey("not.exists")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> 
            configService.updateConfig("not.exists", "value"));
    }

    @Test
    void testUpdateConfig_NotEditable() {
        testConfig.setEditable(false);
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        assertThrows(BusinessException.class, () -> 
            configService.updateConfig("test.key", "new-value"));
    }

    @Test
    void testBatchUpdateConfigs_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig config1 = new SysConfig();
            config1.setConfigKey("key1");
            config1.setConfigValue("value1");
            config1.setEditable(true);

            SysConfig config2 = new SysConfig();
            config2.setConfigKey("key2");
            config2.setConfigValue("value2");
            config2.setEditable(true);

            when(configMapper.selectByKey("key1")).thenReturn(config1);
            when(configMapper.selectByKey("key2")).thenReturn(config2);
            when(configMapper.updateById(any(SysConfig.class))).thenReturn(1);
            when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(config1, config2));
            configService.refreshCache();

            Map<String, String> updates = Map.of("key1", "newValue1", "key2", "newValue2");
            assertDoesNotThrow(() -> configService.batchUpdateConfigs(updates));

            verify(configMapper, times(2)).updateById(any(SysConfig.class));
        }
    }

    @Test
    void testCreateConfig_Success() {
        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::getCurrentUserId).thenReturn(1L);

            SysConfig newConfig = new SysConfig();
            newConfig.setConfigKey("new.key");
            newConfig.setConfigValue("new-value");

            when(configMapper.selectByKey("new.key")).thenReturn(null);
            when(configMapper.insert(any(SysConfig.class))).thenReturn(1);

            SysConfig result = configService.createConfig(newConfig);

            assertNotNull(result);
            verify(configMapper).insert(any(SysConfig.class));
        }
    }

    @Test
    void testCreateConfig_KeyExists() {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigKey("test.key");
        newConfig.setConfigValue("new-value");

        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        assertThrows(BusinessException.class, () -> configService.createConfig(newConfig));
    }

    @Test
    void testDeleteConfig_Success() {
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);
        when(configMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> configService.deleteConfig("test.key"));

        verify(configMapper).deleteById(1L);
    }

    @Test
    void testDeleteConfig_NotFound() {
        when(configMapper.selectByKey("not.exists")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> configService.deleteConfig("not.exists"));
    }

    @Test
    void testDeleteConfig_NotEditable() {
        testConfig.setEditable(false);
        when(configMapper.selectByKey("test.key")).thenReturn(testConfig);

        assertThrows(BusinessException.class, () -> configService.deleteConfig("test.key"));
    }

    @Test
    void testRefreshCache() {
        SysConfig config1 = new SysConfig();
        config1.setConfigKey("key1");
        config1.setConfigValue("value1");

        SysConfig config2 = new SysConfig();
        config2.setConfigKey("key2");
        config2.setConfigValue(null); // null value

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(config1, config2));

        configService.refreshCache();

        assertEquals("value1", configService.getValue("key1"));
        assertNull(configService.getValue("key2"));
    }

    @Test
    void testGetArchiveNoPrefix_Specific() {
        SysConfig prefixConfig = new SysConfig();
        prefixConfig.setConfigKey("archive.no.prefix.DOCUMENT");
        prefixConfig.setConfigValue("DOC");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(prefixConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("DOCUMENT");

        assertEquals("DOC", result);
    }

    @Test
    void testGetArchiveNoPrefix_Default() {
        SysConfig defaultConfig = new SysConfig();
        defaultConfig.setConfigKey("archive.no.prefix.DEFAULT");
        defaultConfig.setConfigValue("ARC");

        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(defaultConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("UNKNOWN_TYPE");

        assertEquals("ARC", result);
    }

    @Test
    void testGetArchiveNoPrefix_NoConfig() {
        // 当没有配置时，使用默认的 DEFAULT 前缀
        SysConfig defaultConfig = new SysConfig();
        defaultConfig.setConfigKey("archive.no.prefix.DEFAULT");
        defaultConfig.setConfigValue("ARC");
        
        when(configMapper.selectAllOrdered()).thenReturn(Arrays.asList(defaultConfig));
        configService.refreshCache();

        String result = configService.getArchiveNoPrefix("UNKNOWN_TYPE");

        // 当没有特定类型配置时，使用默认前缀
        assertEquals("ARC", result);
    }
}
