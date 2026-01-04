package com.lawfirm.application.admin.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出函申请DTO
 */
@Data
public class LetterApplicationDTO {
    private Long id;
    private String applicationNo;
    private Long templateId;
    private String templateName;
    private Long matterId;
    private String matterName;
    private String matterNo;
    private Long clientId;
    private String clientName;
    private Long applicantId;
    private String applicantName;
    private Long departmentId;
    private String departmentName;
    private String letterType;
    private String letterTypeName;
    private String targetUnit;
    private String targetContact;
    private String targetPhone;
    private String targetAddress;
    private String purpose;
    private String lawyerIds;
    private String lawyerNames;
    private String content;
    private Integer copies;
    private LocalDate expectedDate;
    private String status;
    private String statusName;
    private Long approvedBy;
    private String approverName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    private Long printedBy;
    private String printerName;
    private LocalDateTime printedAt;
    private Long receivedBy;
    private String receiverName;
    private LocalDateTime receivedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
