package com.archivesystem.dto.borrow;

import com.archivesystem.entity.BorrowApplication;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅申请摘要响应 DTO.
 */
@Value
@Builder
public class BorrowApplicationSummaryResponse {

    Long id;
    String applicationNo;
    String archiveNo;
    String archiveTitle;
    String applicantName;
    String applicantDept;
    LocalDateTime applyTime;
    String borrowType;
    LocalDate expectedReturnDate;
    String status;

    public static BorrowApplicationSummaryResponse from(BorrowApplication application) {
        if (application == null) {
            return null;
        }

        return BorrowApplicationSummaryResponse.builder()
                .id(application.getId())
                .applicationNo(application.getApplicationNo())
                .archiveNo(application.getArchiveNo())
                .archiveTitle(application.getArchiveTitle())
                .applicantName(application.getApplicantName())
                .applicantDept(application.getApplicantDept())
                .applyTime(application.getApplyTime())
                .borrowType(application.getBorrowType())
                .expectedReturnDate(application.getExpectedReturnDate())
                .status(application.getStatus())
                .build();
    }
}
