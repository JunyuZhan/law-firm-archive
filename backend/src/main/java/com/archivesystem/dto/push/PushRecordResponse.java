package com.archivesystem.dto.push;

import com.archivesystem.entity.PushRecord;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 推送记录响应DTO.
 */
@Value
@Builder
public class PushRecordResponse {

    Long id;
    String pushBatchNo;
    String sourceType;
    String sourceId;
    Long archiveId;
    String archiveNo;
    String title;
    String pushStatus;
    Integer totalFiles;
    Integer successFiles;
    String errorMessage;
    LocalDateTime pushedAt;
    LocalDateTime processedAt;

    public static PushRecordResponse from(PushRecord record) {
        if (record == null) {
            return null;
        }

        return PushRecordResponse.builder()
                .id(record.getId())
                .pushBatchNo(record.getPushBatchNo())
                .sourceType(record.getSourceType())
                .sourceId(record.getSourceId())
                .archiveId(record.getArchiveId())
                .archiveNo(record.getArchiveNo())
                .title(record.getTitle())
                .pushStatus(record.getPushStatus())
                .totalFiles(record.getTotalFiles())
                .successFiles(record.getSuccessFiles())
                .errorMessage(record.getErrorMessage())
                .pushedAt(record.getPushedAt())
                .processedAt(record.getProcessedAt())
                .build();
    }
}
