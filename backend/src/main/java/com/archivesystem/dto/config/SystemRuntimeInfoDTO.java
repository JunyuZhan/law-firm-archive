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
}
