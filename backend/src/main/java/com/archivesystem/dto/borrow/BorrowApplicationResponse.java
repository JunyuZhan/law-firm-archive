package com.archivesystem.dto.borrow;

import com.archivesystem.entity.BorrowApplication;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅申请响应DTO，避免暴露内部人员标识和审计字段.
 */
@Value
@Builder
public class BorrowApplicationResponse {

    Long id;
    String applicationNo;
    Long archiveId;
    String archiveNo;
    String archiveTitle;
    String applicantName;
    String applicantDept;
    LocalDateTime applyTime;
    String borrowPurpose;
    String borrowType;
    LocalDate expectedReturnDate;
    LocalDate actualReturnDate;
    LocalDateTime borrowTime;
    Integer renewCount;
    String status;
    String approverName;
    LocalDateTime approveTime;
    String approveRemarks;
    String rejectReason;
    String returnRemarks;
    String remarks;

    public static BorrowApplicationResponse from(BorrowApplication application) {
        if (application == null) {
            return null;
        }

        return BorrowApplicationResponse.builder()
                .id(application.getId())
                .applicationNo(application.getApplicationNo())
                .archiveId(application.getArchiveId())
                .archiveNo(application.getArchiveNo())
                .archiveTitle(application.getArchiveTitle())
                .applicantName(application.getApplicantName())
                .applicantDept(application.getApplicantDept())
                .applyTime(application.getApplyTime())
                .borrowPurpose(application.getBorrowPurpose())
                .borrowType(application.getBorrowType())
                .expectedReturnDate(application.getExpectedReturnDate())
                .actualReturnDate(application.getActualReturnDate())
                .borrowTime(application.getBorrowTime())
                .renewCount(application.getRenewCount())
                .status(application.getStatus())
                .approverName(application.getApproverName())
                .approveTime(application.getApproveTime())
                .approveRemarks(application.getApproveRemarks())
                .rejectReason(application.getRejectReason())
                .returnRemarks(application.getReturnRemarks())
                .remarks(application.getRemarks())
                .build();
    }
}
