package com.archivesystem.dto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统运行时版本信息.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemRuntimeInfoDTO {

    private String applicationName;
    private String productVersion;
    private String backendVersion;
    private String commitSha;
    private String buildTime;
    /**
     * 兼容旧字段：与 upgradeModeDescription 简短版一致。
     */
    private String recommendedMode;

    /**
     * 升级检测方式代码，例如 REGISTRY_DIGEST。
     */
    private String upgradeMode;

    /**
     * 升级检测方式说明（展示在系统信息页）。
     */
    private String upgradeModeDescription;
}
