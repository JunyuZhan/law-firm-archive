package com.archivesystem.dto.archive;

import lombok.Builder;
import lombok.Value;

/**
 * 文件访问地址响应 DTO.
 */
@Value
@Builder
public class FileUrlResponse {

    String url;

    public static FileUrlResponse of(String url) {
        return FileUrlResponse.builder()
                .url(url)
                .build();
    }
}
