package com.lawfirm.application.hr.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 晋升申请 DTO
 */
@Data
public class PromotionApplicationDTO {
    private Long id;
    private String applicationNo;
    
    private Long employeeId;
    private String employeeName;
    private Long departmentId;
    private String departmentName;
    
    private Long currentLevelId;
    private String currentLevelName;
    private Long targetLevelId;
    private String targetLevelName;
    
    private String applyReason;
    private String achievements;
    private String selfEvaluation;
    private List<String> attachments;
    
    private String status;
    private String statusName;
    
    private BigDecimal reviewScore;
    private String reviewResult;
    private String reviewResultName;
    private String reviewComment;
    
    private Long approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String approvalComment;
    
    private LocalDate effectiveDate;
    private LocalDate applyDate;
    
    /**
     * 评审记录列表
     */
    private List<PromotionReviewDTO> reviews;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
