package com.archivesystem.dto.config;

import com.archivesystem.entity.SysConfig;
import lombok.Builder;
import lombok.Data;

/**
 * 公开站点配置响应 DTO.
 */
@Data
@Builder
public class PublicSiteConfigResponse {
    private String configKey;
    private String configValue;

    public static PublicSiteConfigResponse from(SysConfig config) {
        return PublicSiteConfigResponse.builder()
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .build();
    }
}
