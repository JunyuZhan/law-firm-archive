package com.archivesystem.service.impl;

import com.archivesystem.common.util.RegistryUrlUtils;
import com.archivesystem.config.RegistryUpgradeProperties;
import com.archivesystem.dto.config.ImageUpgradeComponentDTO;
import com.archivesystem.dto.config.ImageUpgradeStatusDTO;
import com.archivesystem.registry.DockerRegistryManifestClient;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.RegistryImageUpgradeCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistryImageUpgradeCheckServiceImpl implements RegistryImageUpgradeCheckService {

    private static final String KEY_REGISTRY_BASE = "system.upgrade.registry_base_url";
    private static final String KEY_BACKEND_REPO = "system.upgrade.backend_repository";
    private static final String KEY_FRONTEND_REPO = "system.upgrade.frontend_repository";
    private static final String KEY_REG_USER = "system.upgrade.registry_username";
    private static final String KEY_REG_PASS = "system.upgrade.registry_password";

    private final ConfigService configService;
    private final RegistryUpgradeProperties registryUpgradeProperties;
    private final DockerRegistryManifestClient manifestClient;

    @Value("${APP_VERSION:}")
    private String appVersion;

    @Override
    public ImageUpgradeStatusDTO checkImageUpgrades() {
        String baseRaw = firstNonBlank(configService.getValue(KEY_REGISTRY_BASE),
                registryUpgradeProperties.getDefaultRegistryBaseUrl());
        String registryBase = RegistryUrlUtils.normalizeRegistryBaseUrl(baseRaw);
        if (!StringUtils.hasText(registryBase)) {
            registryBase = RegistryUrlUtils.normalizeRegistryBaseUrl(
                    registryUpgradeProperties.getDefaultRegistryBaseUrl());
        }

        String backendRepo = firstNonBlank(configService.getValue(KEY_BACKEND_REPO),
                registryUpgradeProperties.getDefaultBackendRepository());
        String frontendRepo = firstNonBlank(configService.getValue(KEY_FRONTEND_REPO),
                registryUpgradeProperties.getDefaultFrontendRepository());

        String tag = StringUtils.hasText(appVersion) ? appVersion.trim() : "latest";

        String userRaw = firstNonBlank(configService.getValue(KEY_REG_USER), registryUpgradeProperties.getRegistryUsername());
        String passRaw = firstNonBlank(configService.getValue(KEY_REG_PASS), registryUpgradeProperties.getRegistryPassword());
        Optional<String> regUser = StringUtils.hasText(userRaw) ? Optional.of(userRaw) : Optional.empty();
        Optional<String> regPass = StringUtils.hasText(passRaw) ? Optional.of(passRaw) : Optional.empty();

        String runningBackend = firstNonBlank(registryUpgradeProperties.getRunningBackendDigest(), "");
        String runningFrontend = firstNonBlank(registryUpgradeProperties.getRunningFrontendDigest(), "");

        List<ImageUpgradeComponentDTO> components = new ArrayList<>();
        components.add(checkOne("BACKEND", backendRepo, tag, registryBase, runningBackend, regUser, regPass));
        components.add(checkOne("FRONTEND", frontendRepo, tag, registryBase, runningFrontend, regUser, regPass));

        boolean upgradeRecommended = components.stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getUpgradeAvailable()));

        return ImageUpgradeStatusDTO.builder()
                .registryBaseUrl(registryBase)
                .imageTag(tag)
                .components(components)
                .upgradeRecommended(upgradeRecommended)
                .checkedAt(Instant.now().toString())
                .build();
    }

    private ImageUpgradeComponentDTO checkOne(String role, String repository, String tag, String registryBase,
                                               String runningDigest,
                                               Optional<String> username, Optional<String> password) {
        Optional<String> remote = manifestClient.fetchManifestDigest(registryBase, repository, tag, username, password);
        if (remote.isEmpty()) {
            return ImageUpgradeComponentDTO.builder()
                    .role(role)
                    .repository(repository)
                    .imageTag(tag)
                    .remoteDigest("")
                    .runningDigest(runningDigest)
                    .upgradeAvailable(null)
                    .status("REMOTE_FAILED")
                    .message("无法从仓库读取镜像清单，请检查仓库地址、镜像路径、标签及鉴权配置。")
                    .build();
        }
        if (!StringUtils.hasText(runningDigest)) {
            return ImageUpgradeComponentDTO.builder()
                    .role(role)
                    .repository(repository)
                    .imageTag(tag)
                    .remoteDigest(remote.get())
                    .runningDigest("")
                    .upgradeAvailable(null)
                    .status("NO_RUNNING_DIGEST")
                    .message("未配置运行中镜像摘要。请在部署环境设置 "
                            + ("BACKEND".equals(role) ? "RUNNING_BACKEND_DIGEST" : "RUNNING_FRONTEND_DIGEST")
                            + "（或 app.registry-upgrade 中对应项），以便与仓库摘要比对。")
                    .build();
        }
        boolean same = runningDigest.equalsIgnoreCase(remote.get());
        return ImageUpgradeComponentDTO.builder()
                .role(role)
                .repository(repository)
                .imageTag(tag)
                .remoteDigest(remote.get())
                .runningDigest(runningDigest)
                .upgradeAvailable(!same)
                .status("OK")
                .message(same ? "当前运行镜像与仓库中该标签的摘要一致。" : "仓库中该标签的镜像摘要与当前运行不一致，建议按部署手册拉取新镜像后升级。")
                .build();
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        return b != null ? b.trim() : "";
    }
}
