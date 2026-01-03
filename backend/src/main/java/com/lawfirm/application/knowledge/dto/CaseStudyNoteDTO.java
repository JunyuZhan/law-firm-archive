package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案例学习笔记DTO（M10-013）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseStudyNoteDTO extends BaseDTO {
    private Long caseId;
    private String caseTitle;
    private Long userId;
    private String userName;
    private String noteContent;
    private String keyPoints;
    private String personalInsights;
}

