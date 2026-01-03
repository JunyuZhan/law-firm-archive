package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 问题整改DTO（M10-032）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityIssueDTO extends BaseDTO {
    private String issueNo;
    private Long checkId;
    private Long matterId;
    private String matterName;
    private String issueType;
    private String issueTypeName;
    private String issueDescription;
    private Long responsibleUserId;
    private String responsibleUserName;
    private String status;
    private String statusName;
    private String priority;
    private String priorityName;
    private LocalDate dueDate;
    private String resolution;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
}

