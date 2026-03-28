package com.archivesystem.controller;

import com.archivesystem.entity.SysConfig;
import com.archivesystem.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConfigService configService;

    @InjectMocks
    private ConfigController configController;

    private ObjectMapper objectMapper;
    private SysConfig testConfig;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(configController).build();
        objectMapper = new ObjectMapper();

        testConfig = new SysConfig();
        testConfig.setId(1L);
        testConfig.setConfigKey("test.key");
        testConfig.setConfigValue("test-value");
        testConfig.setConfigGroup(SysConfig.GROUP_SYSTEM);
        testConfig.setDescription("测试配置描述");
    }

    @Test
    void testGetAllGrouped() throws Exception {
        Map<String, List<SysConfig>> grouped = new HashMap<>();
        grouped.put(SysConfig.GROUP_SYSTEM, Arrays.asList(testConfig));

        when(configService.getAllGrouped()).thenReturn(grouped);

        mockMvc.perform(get("/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.SYSTEM[0].configKey").value("test.key"));
    }

    @Test
    void testGetAll() throws Exception {
        when(configService.getAll()).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].configKey").value("test.key"));
    }

    @Test
    void testGetByGroup() throws Exception {
        when(configService.getByGroup("SYSTEM")).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/group/SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].configKey").value("test.key"));
    }

    @Test
    void testGetByKey() throws Exception {
        when(configService.getByKey("test.key")).thenReturn(testConfig);

        mockMvc.perform(get("/configs/test.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.configKey").value("test.key"));
    }

    @Test
    void testUpdateConfig() throws Exception {
        doNothing().when(configService).updateConfig(eq("test.key"), eq("new-value"));

        Map<String, String> body = new HashMap<>();
        body.put("value", "new-value");

        mockMvc.perform(put("/configs/test.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("更新成功"));

        verify(configService).updateConfig("test.key", "new-value");
    }

    @Test
    void testBatchUpdate() throws Exception {
        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        doNothing().when(configService).batchUpdateConfigs(anyMap());

        mockMvc.perform(put("/configs/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configs)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("批量更新成功"));

        verify(configService).batchUpdateConfigs(configs);
    }

    @Test
    void testCreateConfig() throws Exception {
        SysConfig newConfig = new SysConfig();
        newConfig.setConfigKey("new.key");
        newConfig.setConfigValue("new-value");
        newConfig.setConfigGroup(SysConfig.GROUP_SYSTEM);
        newConfig.setDescription("新配置描述");

        SysConfig createdConfig = new SysConfig();
        createdConfig.setId(2L);
        createdConfig.setConfigKey("new.key");
        createdConfig.setConfigValue("new-value");

        when(configService.createConfig(any(SysConfig.class))).thenReturn(createdConfig);

        mockMvc.perform(post("/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("创建成功"))
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void testDeleteConfig() throws Exception {
        doNothing().when(configService).deleteConfig("test.key");

        mockMvc.perform(delete("/configs/test.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(configService).deleteConfig("test.key");
    }

    @Test
    void testRefreshCache() throws Exception {
        doNothing().when(configService).refreshCache();

        mockMvc.perform(post("/configs/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("缓存刷新成功"));

        verify(configService).refreshCache();
    }

    @Test
    void testGetArchiveNoConfigs() throws Exception {
        when(configService.getByGroup(SysConfig.GROUP_ARCHIVE_NO)).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/archive-no"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(configService).getByGroup(SysConfig.GROUP_ARCHIVE_NO);
    }

    @Test
    void testGetRetentionConfigs() throws Exception {
        when(configService.getByGroup(SysConfig.GROUP_RETENTION)).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/retention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(configService).getByGroup(SysConfig.GROUP_RETENTION);
    }

    @Test
    void testGetSystemConfigs() throws Exception {
        when(configService.getByGroup(SysConfig.GROUP_SYSTEM)).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"));

        verify(configService).getByGroup(SysConfig.GROUP_SYSTEM);
    }
}
