package com.archivesystem.service.impl;

import com.archivesystem.common.util.RegistryUrlUtils;
import com.archivesystem.config.RegistryUpgradeProperties;
import com.archivesystem.dto.config.RegistryUpdateCheckDTO;
import com.archivesystem.registry.DockerRegistryManifestClient;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.RegistryUpdateCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistryUpdateCheckServiceImpl implements RegistryUpdateCheckService {

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
    public RegistryUpdateCheckDTO checkRegistryUpdate() {
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

        Part backend = evaluate(backendRepo, tag, registryBase, runningBackend, regUser, regPass);
        Part frontend = evaluate(frontendRepo, tag, registryBase, runningFrontend, regUser, regPass);

        return merge(backend, frontend);
    }

    private Part evaluate(String repository, String tag, String registryBase, String runningDigest,
                          Optional<String> username, Optional<String> password) {
        Optional<String> remote = manifestClient.fetchManifestDigest(registryBase, repository, tag, username, password);
        if (remote.isEmpty()) {
            return new Part(true, null);
        }
        if (!StringUtils.hasText(runningDigest)) {
            return new Part(false, null);
        }
        boolean same = runningDigest.equalsIgnoreCase(remote.get());
        return new Part(false, !same);
    }

    private RegistryUpdateCheckDTO merge(Part backend, Part frontend) {
        String checkedAt = Instant.now().toString();
        if (backend.remoteFailed || frontend.remoteFailed) {
            return RegistryUpdateCheckDTO.builder()
                    .updateAvailable(null)
                    .message("无法从镜像仓库获取更新信息，请检查系统配置中的仓库地址与账号。")
                    .checkedAt(checkedAt)
                    .build();
        }
        boolean anyUpdate = Boolean.TRUE.equals(backend.upgrade) || Boolean.TRUE.equals(frontend.upgrade);
        if (anyUpdate) {
            return RegistryUpdateCheckDTO.builder()
                    .updateAvailable(true)
                    .message("镜像仓库中存在可用更新。")
                    .checkedAt(checkedAt)
                    .build();
        }
        boolean unknown = backend.upgrade == null || frontend.upgrade == null;
        if (unknown) {
            return RegistryUpdateCheckDTO.builder()
                    .updateAvailable(null)
                    .message("已连接镜像仓库；配置运行镜像摘要后即可判断是否可用更新。")
                    .checkedAt(checkedAt)
                    .build();
        }
        return RegistryUpdateCheckDTO.builder()
                .updateAvailable(false)
                .message("镜像仓库中暂无可用的更新。")
                .checkedAt(checkedAt)
                .build();
    }

    private record Part(boolean remoteFailed, Boolean upgrade) {
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        return b != null ? b.trim() : "";
    }
}
