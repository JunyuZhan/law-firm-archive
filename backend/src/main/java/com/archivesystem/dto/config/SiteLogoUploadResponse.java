package com.archivesystem.dto.config;

import lombok.Builder;
import lombok.Value;

/**
 * 站点 Logo 上传响应 DTO.
 */
@Value
@Builder
public class SiteLogoUploadResponse {

    String logoUrl;
    String objectName;
}
