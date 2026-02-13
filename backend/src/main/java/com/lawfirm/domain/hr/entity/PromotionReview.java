package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 晋升评审记录实体. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("hr_promotion_review")
public class PromotionReview implements Serializable {

  /** 主键ID. */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 晋升申请ID. */
  private Long applicationId;

  /** 评审人ID. */
  private Long reviewerId;

  /** 评审人姓名. */
  private String reviewerName;

  /** 评审人角色：DIRECT_MANAGER-直属上级, HR-人力资源, TEAM_LEADER-团队负责人, COMMITTEE-评审委员会. */
  private String reviewerRole;

  /** 评分明细（JSON格式）. */
  private String scoreDetails;

  /** 总分. */
  private BigDecimal totalScore;

  /** 评审意见：APPROVE-同意, REJECT-不同意, ABSTAIN-弃权. */
  private String reviewOpinion;

  /** 评审评语. */
  private String reviewComment;

  /** 评审时间. */
  private LocalDateTime reviewTime;

  /** 创建时间. */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createdAt;
}
