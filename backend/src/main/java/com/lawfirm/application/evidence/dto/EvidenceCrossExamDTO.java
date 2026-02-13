package com.lawfirm.application.evidence.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 质证记录DTO. */
@Data
public class EvidenceCrossExamDTO {

  /** ID */
  private Long id;

  /** 证据ID */
  private Long evidenceId;

  /** 质证方 */
  private String examParty;

  /** 质证方名称 */
  private String examPartyName;

  /** 真实性意见 */
  private String authenticityOpinion;

  /** 真实性意见名称 */
  private String authenticityOpinionName;

  /** 真实性理由 */
  private String authenticityReason;

  /** 合法性意见 */
  private String legalityOpinion;

  /** 合法性意见名称 */
  private String legalityOpinionName;

  /** 合法性理由 */
  private String legalityReason;

  /** 关联性意见 */
  private String relevanceOpinion;

  /** 关联性意见名称 */
  private String relevanceOpinionName;

  /** 关联性理由 */
  private String relevanceReason;

  /** 综合意见 */
  private String overallOpinion;

  /** 法院意见 */
  private String courtOpinion;

  /** 是否被法院采纳 */
  private Boolean courtAccepted;

  /** 创建人ID */
  private Long createdBy;

  /** 创建人姓名 */
  private String createdByName;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
