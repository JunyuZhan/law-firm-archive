package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 法规DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LawRegulationDTO extends BaseDTO {
    private Long id;
    private String title;
    private Long categoryId;
    private String categoryName;
    private String docNumber;
    private String issuingAuthority;
    private LocalDate issueDate;
    private LocalDate effectiveDate;
    private LocalDate expiryDate;
    private String status;
    private String statusName;
    private String content;
    private String summary;
    private String keywords;
    private String source;
    private String attachmentUrl;
    private Integer viewCount;
    private Integer collectCount;
    private Boolean collected;
    private LocalDateTime createdAt;
}
