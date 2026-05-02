package com.archivesystem.dto.source;

import com.archivesystem.entity.ExternalSource;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 外部来源响应DTO.
 */
@Value
@Builder
public class ExternalSourceResponse {

    Long id;
    String sourceCode;
    String sourceName;
    String sourceType;
    String description;
    String apiUrl;
    String apiKey;
    String authType;
    Map<String, Object> extraConfig;
    Boolean enabled;
    LocalDateTime lastSyncAt;
    String lastSyncStatus;

    public static ExternalSourceResponse from(ExternalSource source, boolean includeApiKey, boolean includeExtraConfig) {
        if (source == null) {
            return null;
        }

        return ExternalSourceResponse.builder()
                .id(source.getId())
                .sourceCode(source.getSourceCode())
                .sourceName(source.getSourceName())
                .sourceType(source.getSourceType())
                .description(source.getDescription())
                .apiUrl(source.getApiUrl())
                .apiKey(includeApiKey ? source.getApiKey() : null)
                .authType(source.getAuthType())
                .extraConfig(includeExtraConfig ? source.getExtraConfig() : null)
                .enabled(source.getEnabled())
                .lastSyncAt(source.getLastSyncAt())
                .lastSyncStatus(source.getLastSyncStatus())
                .build();
    }
}
