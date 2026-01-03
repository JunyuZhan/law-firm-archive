package com.lawfirm.application.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 案源跟进记录 DTO
 */
@Data
public class LeadFollowUpDTO {

    private Long id;
    private Long leadId;
    private String followType;
    private String followContent;
    private String followResult;
    private LocalDateTime nextFollowTime;
    private String nextFollowPlan;
    private Long followUserId;
    private String followUserName;
    private LocalDateTime createdAt;
}

