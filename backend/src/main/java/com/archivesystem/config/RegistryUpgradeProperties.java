package com.archivesystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 镜像仓库更新检测（环境变量与默认值）。
 */
@Data
@ConfigurationProperties(prefix = "app.registry-upgrade")
public class RegistryUpgradeProperties {

    /**
     * 当前运行的后端镜像 digest（如 sha256:…），与仓库 manifest 比对。
     */
    private String runningBackendDigest = "";

    /**
     * 当前运行的前端镜像 digest。
     */
    private String runningFrontendDigest = "";

    private String defaultRegistryBaseUrl = "https://hub.albertzhan.top";

    private String defaultBackendRepository = "law-firm-archive/backend";

    private String defaultFrontendRepository = "law-firm-archive/frontend";

    private String registryUsername = "";

    private String registryPassword = "";
}
