package com.lawfirm.application.evidence.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质证记录DTO
 */
@Data
public class EvidenceCrossExamDTO {

    private Long id;
    private Long evidenceId;
    
    private String examParty;
    private String examPartyName;
    
    private String authenticityOpinion;
    private String authenticityOpinionName;
    private String authenticityReason;
    
    private String legalityOpinion;
    private String legalityOpinionName;
    private String legalityReason;
    
    private String relevanceOpinion;
    private String relevanceOpinionName;
    private String relevanceReason;
    
    private String overallOpinion;
    
    private String courtOpinion;
    private Boolean courtAccepted;
    
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
