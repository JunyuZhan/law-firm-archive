package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案例库DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseLibraryDTO extends BaseDTO {
    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String caseNumber;
    private String courtName;
    private LocalDate judgeDate;
    private String caseType;
    private String causeOfAction;
    private String trialProcedure;
    private String plaintiff;
    private String defendant;
    private String caseSummary;
    private String courtOpinion;
    private String judgmentResult;
    private String caseSignificance;
    private String keywords;
    private String source;
    private String sourceName;
    private Long matterId;
    private String attachmentUrl;
    private Integer viewCount;
    private Integer collectCount;
    private Boolean collected;
    private LocalDateTime createdAt;
}
