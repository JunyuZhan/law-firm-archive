package com.archivesystem.dto.push;

import com.archivesystem.entity.PushRecord;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 推送记录摘要响应 DTO.
 */
@Value
@Builder
public class PushRecordSummaryResponse {

    Long id;
    String pushBatchNo;
    String sourceType;
    Long archiveId;
    String archiveNo;
    String title;
    String pushStatus;
    Integer totalFiles;
    Integer successFiles;
    LocalDateTime pushedAt;

    public static PushRecordSummaryResponse from(PushRecord record) {
        if (record == null) {
            return null;
        }

        return PushRecordSummaryResponse.builder()
                .id(record.getId())
                .pushBatchNo(record.getPushBatchNo())
                .sourceType(record.getSourceType())
                .archiveId(record.getArchiveId())
                .archiveNo(record.getArchiveNo())
                .title(record.getTitle())
                .pushStatus(record.getPushStatus())
                .totalFiles(record.getTotalFiles())
                .successFiles(record.getSuccessFiles())
                .pushedAt(record.getPushedAt())
                .build();
    }
}
