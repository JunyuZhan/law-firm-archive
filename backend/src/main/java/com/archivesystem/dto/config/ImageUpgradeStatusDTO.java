package com.archivesystem.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUpgradeStatusDTO {

    private String registryBaseUrl;

    private String imageTag;

    private List<ImageUpgradeComponentDTO> components;

    /**
     * 任一组件明确判定有新镜像时为 true。
     */
    private boolean upgradeRecommended;

    private String checkedAt;
}
