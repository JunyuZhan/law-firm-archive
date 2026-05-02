package com.archivesystem.service.impl;

import com.archivesystem.common.util.RegistryUrlUtils;
import com.archivesystem.config.DistCenterProperties;
import com.archivesystem.config.RegistryUpgradeProperties;
import com.archivesystem.dto.config.RegistryUpdateCheckDTO;
import com.archivesystem.registry.DockerRegistryManifestClient;
import com.archivesystem.registry.ManifestFetchResult;
import com.archivesystem.registry.distcenter.DistCenterDescriptor;
import com.archivesystem.registry.distcenter.DistCenterLatestFetcher;
import com.archivesystem.registry.distcenter.DistCenterLatestOutcome;
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
    private static final String KEY_DIST_CENTER_LATEST = "system.upgrade.dist_center_latest_json_url";
    private static final String DIST_CENTER_FAILURE_PUBLIC_MESSAGE = "拉取分发中心版本描述失败，请检查地址配置、网络连接和返回内容";

    private final ConfigService configService;
    private final RegistryUpgradeProperties registryUpgradeProperties;
    private final DockerRegistryManifestClient manifestClient;
    private final DistCenterLatestFetcher distCenterLatestFetcher;
    private final DistCenterProperties distCenterProperties;

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

        RegistryUpdateCheckDTO dto = merge(backend, frontend);
        return applyDistCenter(dto);
    }

    private RegistryUpdateCheckDTO applyDistCenter(RegistryUpdateCheckDTO dto) {
        String localTag = StringUtils.hasText(appVersion) ? appVersion.trim() : "";
        dto.setLocalImageTag(StringUtils.hasText(localTag) ? localTag : null);

        String url = firstNonBlank(configService.getValue(KEY_DIST_CENTER_LATEST), distCenterProperties.getLatestJsonUrl());
        if (!StringUtils.hasText(url)) {
            dto.setDistCenterConfigured(false);
            return dto;
        }

        dto.setDistCenterConfigured(true);
        DistCenterLatestOutcome outcome = distCenterLatestFetcher.fetchDescriptor(url.trim());
        if (!outcome.success()) {
            dto.setDistCenterOk(false);
            String err = sanitizeDistCenterError(outcome.errorMessage());
            dto.setDistCenterError(err);
            dto.setDetail(appendDetail(dto.getDetail(), "分发中心：" + err));
            return dto;
        }

        dto.setDistCenterOk(true);
        dto.setDistCenterError(null);
        DistCenterDescriptor d = outcome.descriptor();
        if (d == null) {
            return dto;
        }
        dto.setDistCenterPublishedImageTag(blankToNull(d.publishedImageTag()));
        dto.setDistCenterSnapshotVersion(blankToNull(d.snapshotVersion()));
        dto.setDistCenterChannelAppVersion(blankToNull(d.channelAppVersion()));

        String pub = StringUtils.hasText(d.publishedImageTag()) ? d.publishedImageTag().trim() : "";
        if (StringUtils.hasText(pub) && StringUtils.hasText(localTag) && !pub.equalsIgnoreCase(localTag)) {
            dto.setUpdateAvailable(true);
            String lead = "分发中心发布版本 → " + pub + "，当前运行版本 → " + localTag + "。";
            dto.setMessage(lead + (StringUtils.hasText(dto.getMessage()) ? " " + dto.getMessage() : ""));
        }
        return dto;
    }

    private static String appendDetail(String existing, String extra) {
        if (!StringUtils.hasText(extra)) {
            return existing;
        }
        if (!StringUtils.hasText(existing)) {
            return extra;
        }
        return existing + " " + extra;
    }

    private Part evaluate(String repository, String tag, String registryBase, String runningDigest,
                          Optional<String> username, Optional<String> password) {
        ManifestFetchResult remote = manifestClient.fetchManifestDigest(registryBase, repository, tag, username, password);
        if (!remote.isSuccess()) {
            String hint = sanitizeRegistryFailureHint(remote.getFailureHint());
            return new Part(false, null, hint);
        }
        if (!StringUtils.hasText(runningDigest)) {
            return new Part(true, null, null);
        }
        boolean same = runningDigest.equalsIgnoreCase(remote.getDigest());
        return new Part(true, !same, null);
    }

    private RegistryUpdateCheckDTO merge(Part backend, Part frontend) {
        String checkedAt = Instant.now().toString();
        String detail = failureDetail(backend, frontend);

        if (!backend.remoteOk && !frontend.remoteOk) {
            return dto(null,
                    "无法从镜像仓库获取更新信息。请检查「规则与运行参数」中的仓库地址、鉴权与镜像路径。",
                    blankToNull(detail),
                    checkedAt);
        }

        boolean anyUpdate = (backend.remoteOk && Boolean.TRUE.equals(backend.upgrade))
                || (frontend.remoteOk && Boolean.TRUE.equals(frontend.upgrade));
        if (anyUpdate) {
            String msg = "镜像仓库中存在可用更新。";
            if (!backend.remoteOk || !frontend.remoteOk) {
                msg += "（另有镜像仓库检查失败，请查看详情。）";
            }
            return dto(true, msg, blankToNull(detail), checkedAt);
        }

        if (backend.remoteOk && frontend.remoteOk) {
            boolean unknown = backend.upgrade == null || frontend.upgrade == null;
            if (unknown) {
                return dto(null, "已连接镜像仓库；配置运行镜像摘要后即可判断是否可用更新。", null, checkedAt);
            }
            return dto(false, "镜像仓库中暂无可用的更新。", null, checkedAt);
        }

        return dto(null,
                "无法完整判断更新状态：部分镜像未能从仓库读取或未配置运行摘要。",
                blankToNull(detail),
                checkedAt);
    }

    private static RegistryUpdateCheckDTO dto(Boolean updateAvailable, String message, String detail, String checkedAt) {
        return RegistryUpdateCheckDTO.builder()
                .updateAvailable(updateAvailable)
                .message(message)
                .detail(detail)
                .checkedAt(checkedAt)
                .build();
    }

    private static String failureDetail(Part backend, Part frontend) {
        StringBuilder sb = new StringBuilder();
        if (!backend.remoteOk) {
            sb.append("后端：").append(nullToPlain(backend.failureHint()));
        }
        if (!frontend.remoteOk) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("前端：").append(nullToPlain(frontend.failureHint()));
        }
        return sb.toString();
    }

    private static String nullToPlain(String s) {
        return StringUtils.hasText(s) ? s : "（无具体说明）";
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }

    private static String sanitizeDistCenterError(String errorMessage) {
        if (!StringUtils.hasText(errorMessage)) {
            return "未知错误";
        }
        String normalized = errorMessage.trim();
        if (normalized.startsWith("HTTP ")) {
            return normalized;
        }
        if ("分发中心地址必须以 http:// 或 https:// 开头".equals(normalized)) {
            return normalized;
        }
        return DIST_CENTER_FAILURE_PUBLIC_MESSAGE;
    }

    private static String sanitizeRegistryFailureHint(String failureHint) {
        if (!StringUtils.hasText(failureHint)) {
            return "未知错误";
        }
        String normalized = failureHint.trim();
        if (normalized.startsWith("HTTP 401")) {
            return normalized;
        }
        if (normalized.startsWith("HTTP 404")) {
            return normalized;
        }
        if (normalized.startsWith("HTTP ")) {
            return "镜像仓库请求失败，请检查仓库地址、镜像路径和网络连通性";
        }
        if (normalized.startsWith("网络或 TLS 异常")) {
            return "镜像仓库网络或 TLS 异常，请检查仓库地址、证书和网络连通性";
        }
        return "镜像仓库检查失败，请检查规则与运行参数配置";
    }

    /**
     * @param remoteOk   是否成功从仓库拉取清单
     * @param upgrade    与运行中 digest 比较是否有更新；remoteOk 且未配置 digest 时为 null
     * @param failureHint remoteOk 为 false 时的原因
     */
    private record Part(boolean remoteOk, Boolean upgrade, String failureHint) {
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        return b != null ? b.trim() : "";
    }
}
