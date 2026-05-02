package com.archivesystem.service.impl;

import com.archivesystem.config.DistCenterProperties;
import com.archivesystem.config.RegistryUpgradeProperties;
import com.archivesystem.dto.config.RegistryUpdateCheckDTO;
import com.archivesystem.registry.DockerRegistryManifestClient;
import com.archivesystem.registry.ManifestFetchResult;
import com.archivesystem.registry.distcenter.DistCenterDescriptor;
import com.archivesystem.registry.distcenter.DistCenterLatestFetcher;
import com.archivesystem.registry.distcenter.DistCenterLatestOutcome;
import com.archivesystem.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistryUpdateCheckServiceImplTest {

    private static final String KEY_REGISTRY_BASE = "system.upgrade.registry_base_url";
    private static final String KEY_BACKEND_REPO = "system.upgrade.backend_repository";
    private static final String KEY_FRONTEND_REPO = "system.upgrade.frontend_repository";
    private static final String KEY_REG_USER = "system.upgrade.registry_username";
    private static final String KEY_REG_PASS = "system.upgrade.registry_password";
    private static final String KEY_DIST_CENTER_LATEST = "system.upgrade.dist_center_latest_json_url";

    @Mock
    private ConfigService configService;
    @Mock
    private RegistryUpgradeProperties registryUpgradeProperties;
    @Mock
    private DockerRegistryManifestClient manifestClient;
    @Mock
    private DistCenterLatestFetcher distCenterLatestFetcher;
    @Mock
    private DistCenterProperties distCenterProperties;

    @InjectMocks
    private RegistryUpdateCheckServiceImpl service;

    @BeforeEach
    void setAppVersion() {
        ReflectionTestUtils.setField(service, "appVersion", "v0.1.0");
    }

    @BeforeEach
    void stubRegistryChain() {
        when(configService.getValue(KEY_REGISTRY_BASE)).thenReturn("");
        when(configService.getValue(KEY_BACKEND_REPO)).thenReturn("");
        when(configService.getValue(KEY_FRONTEND_REPO)).thenReturn("");
        when(configService.getValue(KEY_REG_USER)).thenReturn("");
        when(configService.getValue(KEY_REG_PASS)).thenReturn("");
        when(configService.getValue(KEY_DIST_CENTER_LATEST)).thenReturn("");

        when(registryUpgradeProperties.getDefaultRegistryBaseUrl()).thenReturn("https://hub.albertzhan.top");
        when(registryUpgradeProperties.getDefaultBackendRepository()).thenReturn("law-firm-archive/backend");
        when(registryUpgradeProperties.getDefaultFrontendRepository()).thenReturn("law-firm-archive/frontend");
        when(registryUpgradeProperties.getRunningBackendDigest()).thenReturn("sha256:aa");
        when(registryUpgradeProperties.getRunningFrontendDigest()).thenReturn("sha256:bb");
        when(registryUpgradeProperties.getRegistryUsername()).thenReturn("");
        when(registryUpgradeProperties.getRegistryPassword()).thenReturn("");

        when(manifestClient.fetchManifestDigest(anyString(), eq("law-firm-archive/backend"), eq("v0.1.0"), any(), any()))
                .thenReturn(ManifestFetchResult.ok("sha256:aa"));
        when(manifestClient.fetchManifestDigest(anyString(), eq("law-firm-archive/frontend"), eq("v0.1.0"), any(), any()))
                .thenReturn(ManifestFetchResult.ok("sha256:bb"));
    }

    @Test
    void whenDistCenterPublishesNewerImageTag_updateAvailable() {
        when(configService.getValue(KEY_DIST_CENTER_LATEST))
                .thenReturn("https://install.example/projects/law-firm-archive/versions/latest.json");
        when(distCenterLatestFetcher.fetchDescriptor("https://install.example/projects/law-firm-archive/versions/latest.json"))
                .thenReturn(DistCenterLatestOutcome.ok(new DistCenterDescriptor("v0.2.0", "bootstrap-1", "latest")));

        RegistryUpdateCheckDTO dto = service.checkRegistryUpdate();

        assertTrue(dto.getUpdateAvailable());
        assertEquals("v0.2.0", dto.getDistCenterPublishedImageTag());
        assertEquals("v0.1.0", dto.getLocalImageTag());
        assertTrue(dto.getDistCenterOk());
    }

    @Test
    void whenDistCenterTagMatchesRegistryNoDigestUpdate_thenNoUpdate() {
        when(configService.getValue(KEY_DIST_CENTER_LATEST)).thenReturn("https://install.example/latest.json");
        when(distCenterLatestFetcher.fetchDescriptor("https://install.example/latest.json"))
                .thenReturn(DistCenterLatestOutcome.ok(new DistCenterDescriptor("v0.1.0", "bootstrap-1", "v0.1.0")));

        RegistryUpdateCheckDTO dto = service.checkRegistryUpdate();

        assertFalse(dto.getUpdateAvailable());
        assertEquals("v0.1.0", dto.getDistCenterPublishedImageTag());
    }

    @Test
    void whenDistCenterFetchFails_shouldHideInternalErrorDetails() {
        when(configService.getValue(KEY_DIST_CENTER_LATEST)).thenReturn("https://install.example/latest.json");
        when(distCenterLatestFetcher.fetchDescriptor("https://install.example/latest.json"))
                .thenReturn(DistCenterLatestOutcome.fail("connect timeout to https://internal.example/latest.json"));

        RegistryUpdateCheckDTO dto = service.checkRegistryUpdate();

        assertFalse(dto.getDistCenterOk());
        assertEquals("拉取分发中心版本描述失败，请检查地址配置、网络连接和返回内容", dto.getDistCenterError());
        assertTrue(dto.getDetail().contains("分发中心：拉取分发中心版本描述失败，请检查地址配置、网络连接和返回内容"));
        assertFalse(dto.getDetail().contains("internal.example"));
    }

    @Test
    void whenRegistryManifestFails_shouldHideInternalUrlDetails() {
        when(manifestClient.fetchManifestDigest(anyString(), eq("law-firm-archive/backend"), eq("v0.1.0"), any(), any()))
                .thenReturn(ManifestFetchResult.fail("HTTP 500：无法读取清单（registry.internal…/v2/backend/manifests/v0.1.0）"));
        when(manifestClient.fetchManifestDigest(anyString(), eq("law-firm-archive/frontend"), eq("v0.1.0"), any(), any()))
                .thenReturn(ManifestFetchResult.ok("sha256:bb"));

        RegistryUpdateCheckDTO dto = service.checkRegistryUpdate();

        assertEquals("无法完整判断更新状态：部分镜像未能从仓库读取或未配置运行摘要。", dto.getMessage());
        assertTrue(dto.getDetail().contains("后端：镜像仓库请求失败，请检查仓库地址、镜像路径和网络连通性"));
        assertFalse(dto.getDetail().contains("registry.internal"));
    }
}
