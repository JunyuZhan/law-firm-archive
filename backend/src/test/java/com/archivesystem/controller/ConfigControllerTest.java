package com.archivesystem.controller;

import com.archivesystem.entity.SysConfig;
import com.archivesystem.dto.config.RegistryUpdateCheckDTO;
import com.archivesystem.service.AlertService;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
import com.archivesystem.service.RegistryUpdateCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.io.ByteArrayInputStream;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConfigService configService;

    @Mock
    private AlertService alertService;

    @Mock
    private MinioService minioService;

    @Mock
    private ObjectProvider<BuildProperties> buildPropertiesProvider;

    @Mock
    private ObjectProvider<HealthEndpoint> healthEndpointProvider;

    @Mock
    private RegistryUpdateCheckService registryUpdateCheckService;

    @InjectMocks
    private ConfigController configController;

    private ObjectMapper objectMapper;
    private SysConfig testConfig;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(configController).build();
        objectMapper = new ObjectMapper();
        lenient().when(buildPropertiesProvider.getIfAvailable()).thenReturn(null);
        lenient().when(healthEndpointProvider.getIfAvailable()).thenReturn(null);
        ReflectionTestUtils.setField(configController, "applicationName", "archive-system");
        ReflectionTestUtils.setField(configController, "appVersion", "v0.1.7");
        ReflectionTestUtils.setField(configController, "appCommitSha", "8fe373f8");
        ReflectionTestUtils.setField(configController, "appBuildTime", "2026-03-31T00:24:52+08:00");

        testConfig = new SysConfig();
        testConfig.setId(1L);
        testConfig.setConfigKey("test.key");
        testConfig.setConfigValue("test-value");
        testConfig.setConfigGroup(SysConfig.GROUP_SYSTEM);
        testConfig.setDescription("测试配置描述");
        testConfig.setCreatedBy(7L);
        testConfig.setUpdatedBy(8L);
    }

    @Test
    void testGetAllGrouped() throws Exception {
        Map<String, List<SysConfig>> grouped = new HashMap<>();
        grouped.put(SysConfig.GROUP_SYSTEM, Arrays.asList(testConfig));

        when(configService.getAllGrouped()).thenReturn(grouped);

        mockMvc.perform(get("/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.SYSTEM[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.SYSTEM[0].configKey").value("test.key"))
                .andExpect(jsonPath("$.data.SYSTEM[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.SYSTEM[0].updatedBy").doesNotExist());
    }

    @Test
    void testGetAll() throws Exception {
        when(configService.getAll()).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].configKey").value("test.key"))
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist());
    }

    @Test
    void testGetByGroup() throws Exception {
        when(configService.getByGroup("SYSTEM")).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/group/SYSTEM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].configKey").value("test.key"))
                .andExpect(jsonPath("$.data[0].updatedBy").doesNotExist());
    }

    @Test
    void testGetPublicSiteConfig_ShouldOnlyReturnAllowlistedKeys() throws Exception {
        SysConfig publicConfig = new SysConfig();
        publicConfig.setConfigKey("system.site.name");
        publicConfig.setConfigValue("档案系统");
        publicConfig.setConfigGroup("SITE");
        publicConfig.setEditable(true);
        publicConfig.setSortOrder(99);

        SysConfig internalConfig = new SysConfig();
        internalConfig.setConfigKey("system.site.logo.object");
        internalConfig.setConfigValue("******");
        internalConfig.setConfigGroup("SITE");

        when(configService.getByGroup("SITE")).thenReturn(Arrays.asList(publicConfig, internalConfig));

        mockMvc.perform(get("/configs/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].configKey").value("system.site.name"))
                .andExpect(jsonPath("$.data[0].configValue").value("档案系统"))
                .andExpect(jsonPath("$.data[0].editable").doesNotExist())
                .andExpect(jsonPath("$.data[0].sortOrder").doesNotExist());
    }

    @Test
    void testGetPublicSiteLogo_ShouldRejectNonLogoObjectPath() throws Exception {
        when(configService.getValue("system.site.logo.object")).thenReturn("archives/private/file.pdf");

        mockMvc.perform(get("/configs/public/site/logo"))
                .andExpect(status().isNotFound());

        verify(minioService, never()).getFile(anyString());
    }

    @Test
    void testGetPublicSiteLogo_ShouldRejectNonImageContentType() throws Exception {
        when(configService.getValue("system.site.logo.object")).thenReturn("site/logo/test.bin");
        when(minioService.getContentType("site/logo/test.bin")).thenReturn("application/pdf");

        mockMvc.perform(get("/configs/public/site/logo"))
                .andExpect(status().isNotFound());

        verify(minioService, never()).getFile("site/logo/test.bin");
    }

    @Test
    void testGetPublicSiteLogo_ShouldReturnImageStream() throws Exception {
        when(configService.getValue("system.site.logo.object")).thenReturn("site/logo/test.png");
        when(minioService.getContentType("site/logo/test.png")).thenReturn("image/png");
        when(minioService.getFile("site/logo/test.png")).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));

        mockMvc.perform(get("/configs/public/site/logo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("must-revalidate")));
    }

    @Test
    void testGetByKey() throws Exception {
        when(configService.getByKey("test.key")).thenReturn(testConfig);

        mockMvc.perform(get("/configs/test.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.configKey").value("test.key"))
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedBy").doesNotExist());
    }

    @Test
    void testGetByKey_SensitiveValueShouldStayRedacted() throws Exception {
        SysConfig sensitiveConfig = new SysConfig();
        sensitiveConfig.setConfigKey("security.crypto.secret");
        sensitiveConfig.setConfigValue("******");

        when(configService.getByKey("security.crypto.secret")).thenReturn(sensitiveConfig);

        mockMvc.perform(get("/configs/security.crypto.secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("******"));
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
    void testUploadSiteLogo() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(multipart("/configs/site/logo")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.message").value("Logo上传成功"))
                .andExpect(jsonPath("$.data.logoUrl").value(org.hamcrest.Matchers.startsWith("/api/configs/public/site/logo?t=")))
                .andExpect(jsonPath("$.data.objectName").value(org.hamcrest.Matchers.startsWith("site/logo/")));

        verify(minioService).uploadFile(eq(file), argThat(name -> name.startsWith("site/logo/") && name.endsWith(".png")));
        verify(configService).saveConfig(eq("system.site.logo.object"), argThat(name -> name.startsWith("site/logo/")), eq("SITE"),
                eq("Logo 对象路径"), eq(SysConfig.TYPE_STRING), eq(false), eq(6));
        verify(configService).saveConfig(eq("system.site.logo"), argThat(url -> url.startsWith("/api/configs/public/site/logo?t=")), eq("SITE"),
                eq("Logo URL"), eq(SysConfig.TYPE_STRING), eq(true), eq(5));
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
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist());
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
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist());

        verify(configService).getByGroup(SysConfig.GROUP_ARCHIVE_NO);
    }

    @Test
    void testGetRetentionConfigs() throws Exception {
        when(configService.getByGroup(SysConfig.GROUP_RETENTION)).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/retention"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].updatedBy").doesNotExist());

        verify(configService).getByGroup(SysConfig.GROUP_RETENTION);
    }

    @Test
    void testGetSystemConfigs() throws Exception {
        when(configService.getByGroup(SysConfig.GROUP_SYSTEM)).thenReturn(Arrays.asList(testConfig));

        mockMvc.perform(get("/configs/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist());

        verify(configService).getByGroup(SysConfig.GROUP_SYSTEM);
    }

    @Test
    void testCheckRegistryUpdate() throws Exception {
        RegistryUpdateCheckDTO dto = RegistryUpdateCheckDTO.builder()
                .updateAvailable(false)
                .message("镜像仓库中暂无可用的更新。")
                .checkedAt("2026-01-01T00:00:00Z")
                .build();
        when(registryUpdateCheckService.checkRegistryUpdate()).thenReturn(dto);

        mockMvc.perform(get("/configs/registry-update-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.updateAvailable").value(false))
                .andExpect(jsonPath("$.data.message").value("镜像仓库中暂无可用的更新。"));

        verify(registryUpdateCheckService).checkRegistryUpdate();
    }

}
