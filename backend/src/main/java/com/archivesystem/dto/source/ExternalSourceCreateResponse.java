package com.archivesystem.dto.source;

import com.archivesystem.entity.ExternalSource;
import lombok.Builder;
import lombok.Value;

/**
 * 外部来源创建响应 DTO.
 */
@Value
@Builder
public class ExternalSourceCreateResponse {

    Long id;
    String sourceCode;
    String sourceName;
    String apiKey;

    public static ExternalSourceCreateResponse from(ExternalSource source) {
        if (source == null) {
            return null;
        }

        return ExternalSourceCreateResponse.builder()
                .id(source.getId())
                .sourceCode(source.getSourceCode())
                .sourceName(source.getSourceName())
                .apiKey(source.getApiKey())
                .build();
    }
}
