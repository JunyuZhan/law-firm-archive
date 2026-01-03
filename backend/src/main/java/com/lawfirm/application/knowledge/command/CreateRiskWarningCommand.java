package com.lawfirm.application.knowledge.command;

import lombok.Data;

/**
 * 创建风险预警命令（M10-033）
 */
@Data
public class CreateRiskWarningCommand {
    private Long matterId;
    private String riskType;
    private String riskLevel;
    private String riskDescription;
    private String warningReason;
    private String suggestedAction;
}

