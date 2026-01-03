package com.lawfirm.application.archive.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 档案借阅 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveBorrowDTO extends BaseDTO {

    private String borrowNo;
    private Long archiveId;
    private String archiveNo;
    private String archiveName;
    private Long borrowerId;
    private String borrowerName;
    private String department;
    private String borrowReason;
    private LocalDate borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDate actualReturnDate;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private Long returnHandlerId;
    private String returnHandlerName;
    private String returnCondition;
    private String returnConditionName;
    private String returnRemarks;
    private Boolean isOverdue;
}

