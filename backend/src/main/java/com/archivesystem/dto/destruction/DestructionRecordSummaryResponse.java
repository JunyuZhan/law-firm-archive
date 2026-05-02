package com.archivesystem.dto.destruction;

import com.archivesystem.entity.DestructionRecord;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 档案销毁记录摘要响应 DTO.
 */
@Data
@Builder
public class DestructionRecordSummaryResponse {
    private Long id;
    private String destructionBatchNo;
    private Long archiveId;
    private String destructionReason;
    private String destructionMethod;
    private String status;
    private String proposerName;
    private LocalDateTime proposedAt;
    private String approverName;
    private LocalDateTime approvedAt;
    private String executorName;
    private LocalDateTime executedAt;

    public static DestructionRecordSummaryResponse from(DestructionRecord record) {
        return DestructionRecordSummaryResponse.builder()
                .id(record.getId())
                .destructionBatchNo(record.getDestructionBatchNo())
                .archiveId(record.getArchiveId())
                .destructionReason(record.getDestructionReason())
                .destructionMethod(record.getDestructionMethod())
                .status(record.getStatus())
                .proposerName(record.getProposerName())
                .proposedAt(record.getProposedAt())
                .approverName(record.getApproverName())
                .approvedAt(record.getApprovedAt())
                .executorName(record.getExecutorName())
                .executedAt(record.getExecutedAt())
                .build();
    }
}
