package com.lawfirm.application.evidence.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建质证记录命令. */
@Data
public class CreateCrossExamCommand {

  /** 证据ID */
  @NotNull(message = "证据ID不能为空")
  private Long evidenceId;

  /** 质证方 */
  @NotBlank(message = "质证方不能为空")
  private String examParty;

  /** 真实性意见 */
  private String authenticityOpinion;

  /** 真实性理由 */
  private String authenticityReason;

  /** 合法性意见 */
  private String legalityOpinion;

  /** 合法性理由 */
  private String legalityReason;

  /** 关联性意见 */
  private String relevanceOpinion;

  /** 关联性理由 */
  private String relevanceReason;

  /** 综合意见 */
  private String overallOpinion;

  /** 法院认定意见 */
  private String courtOpinion;

  /** 法院是否采纳 */
  private Boolean courtAccepted;
}
