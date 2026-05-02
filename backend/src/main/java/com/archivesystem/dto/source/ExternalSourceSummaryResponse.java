package com.archivesystem.dto.source;

import com.archivesystem.entity.ExternalSource;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 外部来源摘要响应 DTO.
 */
@Value
@Builder
public class ExternalSourceSummaryResponse {

    Long id;
    String sourceCode;
    String sourceName;
    String sourceType;
    String apiUrl;
    String authType;
    Boolean enabled;
    LocalDateTime lastSyncAt;
    String lastSyncStatus;

    public static ExternalSourceSummaryResponse from(ExternalSource source) {
        if (source == null) {
            return null;
        }

        return ExternalSourceSummaryResponse.builder()
                .id(source.getId())
                .sourceCode(source.getSourceCode())
                .sourceName(source.getSourceName())
                .sourceType(source.getSourceType())
                .apiUrl(source.getApiUrl())
                .authType(source.getAuthType())
                .enabled(source.getEnabled())
                .lastSyncAt(source.getLastSyncAt())
                .lastSyncStatus(source.getLastSyncStatus())
                .build();
    }
}
