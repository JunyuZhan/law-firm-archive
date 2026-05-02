package com.archivesystem.dto.source;

import lombok.Builder;
import lombok.Value;

/**
 * 外部来源 API Key 响应 DTO.
 */
@Value
@Builder
public class ExternalSourceApiKeyResponse {

    String apiKey;
}
