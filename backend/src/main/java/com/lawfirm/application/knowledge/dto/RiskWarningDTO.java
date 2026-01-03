package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 风险预警DTO（M10-033）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskWarningDTO extends BaseDTO {
    private String warningNo;
    private Long matterId;
    private String matterName;
    private String riskType;
    private String riskTypeName;
    private String riskLevel;
    private String riskLevelName;
    private String riskDescription;
    private String warningReason;
    private String suggestedAction;
    private String status;
    private String statusName;
    private LocalDateTime acknowledgedAt;
    private Long acknowledgedBy;
    private String acknowledgedByName;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private String resolvedByName;
}

