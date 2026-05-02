package com.archivesystem.dto.config;

import com.archivesystem.entity.SysConfig;
import lombok.Builder;
import lombok.Value;

/**
 * 系统配置响应DTO.
 */
@Value
@Builder
public class SysConfigResponse {

    String configKey;
    String configValue;
    String configType;
    String configGroup;
    String description;
    Boolean editable;
    Integer sortOrder;

    public static SysConfigResponse from(SysConfig config) {
        if (config == null) {
            return null;
        }

        return SysConfigResponse.builder()
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType())
                .configGroup(config.getConfigGroup())
                .description(config.getDescription())
                .editable(config.getEditable())
                .sortOrder(config.getSortOrder())
                .build();
    }
}
