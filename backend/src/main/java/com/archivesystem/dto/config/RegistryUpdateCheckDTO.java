package com.archivesystem.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 镜像仓库是否有可用更新（聚合后端与前端镜像检查结果）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryUpdateCheckDTO {

    /**
     * true：存在可用更新；false：暂无；null：当前无法判断。
     */
    private Boolean updateAvailable;

    private String message;

    private String checkedAt;
}
