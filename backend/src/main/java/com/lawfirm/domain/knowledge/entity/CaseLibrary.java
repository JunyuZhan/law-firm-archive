package com.lawfirm.domain.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 案例库实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("case_library")
public class CaseLibrary extends BaseEntity {

  /** 案例标题. */
  private String title;

  /** 分类ID. */
  private Long categoryId;

  /** 案号. */
  private String caseNumber;

  /** 审理法院. */
  private String courtName;

  /** 裁判日期. */
  private LocalDate judgeDate;

  /** 案件类型. */
  private String caseType;

  /** 案由. */
  private String causeOfAction;

  /** 审理程序. */
  private String trialProcedure;

  /** 原告/上诉人. */
  private String plaintiff;

  /** 被告/被上诉人. */
  private String defendant;

  /** 案情摘要. */
  private String caseSummary;

  /** 法院观点. */
  private String courtOpinion;

  /** 裁判结果. */
  private String judgmentResult;

  /** 案例意义. */
  private String caseSignificance;

  /** 关键词. */
  private String keywords;

  /** 来源: EXTERNAL外部/INTERNAL内部. */
  private String source;

  /** 关联案件ID. */
  private Long matterId;

  /** 附件URL. */
  private String attachmentUrl;

  /** 浏览次数. */
  private Integer viewCount;

  /** 收藏次数. */
  private Integer collectCount;

  // 来源常量
  /** 来源：外部. */
  public static final String SOURCE_EXTERNAL = "EXTERNAL";

  /** 来源：内部. */
  public static final String SOURCE_INTERNAL = "INTERNAL";
}
