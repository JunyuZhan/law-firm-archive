package com.archivesystem.dto.retention;

import com.archivesystem.entity.Archive;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 到期保管档案摘要响应 DTO.
 */
@Data
@Builder
public class RetentionArchiveResponse {
    private Long id;
    private String archiveNo;
    private String title;
    private String retentionPeriod;
    private LocalDate retentionExpireDate;

    public static RetentionArchiveResponse from(Archive archive) {
        return RetentionArchiveResponse.builder()
                .id(archive.getId())
                .archiveNo(archive.getArchiveNo())
                .title(archive.getTitle())
                .retentionPeriod(archive.getRetentionPeriod())
                .retentionExpireDate(archive.getRetentionExpireDate())
                .build();
    }
}
