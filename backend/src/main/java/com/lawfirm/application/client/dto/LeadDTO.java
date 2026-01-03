package com.lawfirm.application.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 案源线索 DTO
 */
@Data
public class LeadDTO {

    private Long id;
    private String leadNo;
    private String leadName;
    private String leadType;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String sourceChannel;
    private String sourceDetail;
    private String status;
    private String priority;
    private String businessType;
    private BigDecimal estimatedAmount;
    private String description;
    private LocalDateTime lastFollowTime;
    private LocalDateTime nextFollowTime;
    private Integer followCount;
    private LocalDateTime convertedAt;
    private Long convertedToClientId;
    private String convertedToClientName;
    private Long convertedToMatterId;
    private String convertedToMatterName;
    private Long originatorId;
    private String originatorName;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

