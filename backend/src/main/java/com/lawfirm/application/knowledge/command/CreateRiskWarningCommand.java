package com.lawfirm.application.knowledge.command;

import lombok.Data;

/** 创建风险预警命令（M10-033） */
@Data
public class CreateRiskWarningCommand {
  /** 项目ID */
  private Long matterId;

  /** 风险类型 */
  private String riskType;

  /** 风险等级 */
  private String riskLevel;

  /** 风险描述 */
  private String riskDescription;

  /** 预警原因 */
  private String warningReason;

  /** 建议措施 */
  private String suggestedAction;
}
