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

    /**
     * 失败或部分失败时的补充说明（如 HTTP 状态、鉴权提示等），便于对照系统配置排查。
     */
    private String detail;

    private String checkedAt;
}
