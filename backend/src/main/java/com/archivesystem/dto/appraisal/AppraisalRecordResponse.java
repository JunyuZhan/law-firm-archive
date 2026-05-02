package com.archivesystem.dto.appraisal;

import com.archivesystem.entity.AppraisalRecord;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * 鉴定记录响应DTO.
 */
@Value
@Builder
public class AppraisalRecordResponse {

    Long id;
    Long archiveId;
    String appraisalType;
    String originalValue;
    String newValue;
    String appraisalReason;
    String status;
    String appraiserName;
    LocalDateTime appraisedAt;
    String approverName;
    LocalDateTime approvedAt;
    String approvalComment;

    public static AppraisalRecordResponse from(AppraisalRecord record) {
        if (record == null) {
            return null;
        }

        return AppraisalRecordResponse.builder()
                .id(record.getId())
                .archiveId(record.getArchiveId())
                .appraisalType(record.getAppraisalType())
                .originalValue(record.getOriginalValue())
                .newValue(record.getNewValue())
                .appraisalReason(record.getAppraisalReason())
                .status(record.getStatus())
                .appraiserName(record.getAppraiserName())
                .appraisedAt(record.getAppraisedAt())
                .approverName(record.getApproverName())
                .approvedAt(record.getApprovedAt())
                .approvalComment(record.getApprovalComment())
                .build();
    }
}
