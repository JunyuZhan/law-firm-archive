package com.lawfirm.application.workbench.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批记录 DTO
 */
@Data
public class ApprovalDTO {
    private Long id;
    private String approvalNo;
    private String businessType;
    private String businessTypeName;
    private Long businessId;
    private String businessNo;
    private String businessTitle;
    private Long applicantId;
    private String applicantName;
    private Long approverId;
    private String approverName;
    private String status;
    private String statusName;
    private String comment;
    private LocalDateTime approvedAt;
    private String priority;
    private String priorityName;
    private String urgency;
    private String urgencyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

