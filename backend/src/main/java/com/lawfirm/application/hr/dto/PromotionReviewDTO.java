package com.lawfirm.application.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;

/** 晋升评审记录 DTO. */
@Data
public class PromotionReviewDTO {
  /** 主键ID. */
  private Long id;

  /** 晋升申请ID. */
  private Long applicationId;

  /** 评审人ID. */
  private Long reviewerId;

  /** 评审人姓名. */
  private String reviewerName;

  /** 评审人角色. */
  private String reviewerRole;

  /** 评审人角色名称. */
  private String reviewerRoleName;

  /** 评分明细（JSON格式）. */
  private Map<String, Object> scoreDetails;

  /** 总分. */
  private BigDecimal totalScore;

  /** 评审意见. */
  private String reviewOpinion;

  /** 评审意见名称. */
  private String reviewOpinionName;

  /** 评审评语. */
  private String reviewComment;

  /** 评审时间. */
  private LocalDateTime reviewTime;

  /** 创建时间. */
  private LocalDateTime createdAt;
}
