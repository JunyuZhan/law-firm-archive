package com.archivesystem.dto.appraisal;

import com.archivesystem.entity.AppraisalRecord;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 鉴定记录摘要响应 DTO.
 */
@Data
@Builder
public class AppraisalRecordSummaryResponse {
    private Long id;
    private Long archiveId;
    private String appraisalType;
    private String originalValue;
    private String newValue;
    private String appraisalReason;
    private String status;
    private String appraiserName;
    private LocalDateTime appraisedAt;
    private String approverName;
    private LocalDateTime approvedAt;

    public static AppraisalRecordSummaryResponse from(AppraisalRecord record) {
        return AppraisalRecordSummaryResponse.builder()
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
                .build();
    }
}
