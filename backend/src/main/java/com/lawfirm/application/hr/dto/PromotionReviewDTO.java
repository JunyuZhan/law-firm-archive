package com.lawfirm.application.hr.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 晋升评审记录 DTO
 */
@Data
public class PromotionReviewDTO {
    private Long id;
    private Long applicationId;
    
    private Long reviewerId;
    private String reviewerName;
    private String reviewerRole;
    private String reviewerRoleName;
    
    private Map<String, Object> scoreDetails;
    private BigDecimal totalScore;
    
    private String reviewOpinion;
    private String reviewOpinionName;
    private String reviewComment;
    
    private LocalDateTime reviewTime;
    private LocalDateTime createdAt;
}
