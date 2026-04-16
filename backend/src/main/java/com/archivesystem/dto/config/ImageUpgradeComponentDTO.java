package com.archivesystem.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUpgradeComponentDTO {

    /**
     * BACKEND 或 FRONTEND。
     */
    private String role;

    private String repository;

    private String imageTag;

    private String remoteDigest;

    private String runningDigest;

    /**
     * true：远端与运行摘要不一致（有新镜像）；false：一致；null：无法判断。
     */
    private Boolean upgradeAvailable;

    /**
     * OK、REMOTE_FAILED、NO_RUNNING_DIGEST 等。
     */
    private String status;

    private String message;
}
