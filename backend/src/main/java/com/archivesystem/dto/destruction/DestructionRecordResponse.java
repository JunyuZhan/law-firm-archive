package com.archivesystem.dto.destruction;

import com.archivesystem.entity.DestructionRecord;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 销毁记录响应 DTO，避免暴露内部人员标识和审计元数据.
 */
@Value
@Builder
public class DestructionRecordResponse {

    Long id;
    String destructionBatchNo;
    Long archiveId;
    String destructionReason;
    String destructionMethod;
    String status;
    String proposerName;
    LocalDateTime proposedAt;
    String approverName;
    LocalDateTime approvedAt;
    String approvalComment;
    String executorName;
    LocalDateTime executedAt;

    public static DestructionRecordResponse from(DestructionRecord record) {
        if (record == null) {
            return null;
        }

        return DestructionRecordResponse.builder()
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
                .approvalComment(record.getApprovalComment())
                .executorName(record.getExecutorName())
                .executedAt(record.getExecutedAt())
                .build();
    }
}
