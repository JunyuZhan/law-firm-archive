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

    // --- Dist Center（versions/latest.json）---

    /**
     * 是否配置了分发中心 latest.json URL（环境变量或 system.upgrade.dist_center_latest_json_url）。
     */
    private Boolean distCenterConfigured;

    /**
     * 是否成功拉取并解析 latest.json。
     */
    private Boolean distCenterOk;

    /** 当前运行实例的镜像 / 产品标签（与容器环境变量 APP_VERSION 一致）。 */
    private String localImageTag;

    /** 分发中心 env.APP_VERSION（若无则回退根节点 app_version）。 */
    private String distCenterPublishedImageTag;

    /** 分发中心描述文件中的 version（如 bootstrap-20260418）。 */
    private String distCenterSnapshotVersion;

    /** 分发中心根节点 app_version（渠道展示用）。 */
    private String distCenterChannelAppVersion;

    /** 拉取或解析失败时的说明。 */
    private String distCenterError;
}
