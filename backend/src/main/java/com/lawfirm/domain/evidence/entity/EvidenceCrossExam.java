package com.lawfirm.domain.evidence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

/** 质证记录实体. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("evidence_cross_exam")
public class EvidenceCrossExam implements Serializable {

  /** 主键ID. */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 证据ID. */
  private Long evidenceId;

  /** 质证方：OUR_SIDE-我方, OPPOSITE-对方, COURT-法院. */
  private String examParty;

  /** 真实性意见. */
  private String authenticityOpinion;

  /** 真实性理由. */
  private String authenticityReason;

  /** 合法性意见. */
  private String legalityOpinion;

  /** 合法性理由. */
  private String legalityReason;

  /** 关联性意见. */
  private String relevanceOpinion;

  /** 关联性理由. */
  private String relevanceReason;

  /** 综合意见. */
  private String overallOpinion;

  /** 法院认定意见. */
  private String courtOpinion;

  /** 法院是否采纳. */
  private Boolean courtAccepted;

  /** 创建人ID. */
  private Long createdBy;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
