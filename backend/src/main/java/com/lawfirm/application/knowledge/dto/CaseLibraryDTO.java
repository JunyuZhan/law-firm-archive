package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 案例库DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseLibraryDTO extends BaseDTO {

  /** ID */
  private Long id;

  /** 标题 */
  private String title;

  /** 分类ID */
  private Long categoryId;

  /** 分类名称 */
  private String categoryName;

  /** 案号 */
  private String caseNumber;

  /** 法院名称 */
  private String courtName;

  /** 审判日期 */
  private LocalDate judgeDate;

  /** 案件类型 */
  private String caseType;

  /** 案由 */
  private String causeOfAction;

  /** 审判程序 */
  private String trialProcedure;

  /** 原告 */
  private String plaintiff;

  /** 被告 */
  private String defendant;

  /** 案件摘要 */
  private String caseSummary;

  /** 法院观点 */
  private String courtOpinion;

  /** 判决结果 */
  private String judgmentResult;

  /** 案件意义 */
  private String caseSignificance;

  /** 关键词 */
  private String keywords;

  /** 来源 */
  private String source;

  /** 来源名称 */
  private String sourceName;

  /** 关联项目ID */
  private Long matterId;

  /** 附件URL */
  private String attachmentUrl;

  /** 浏览次数 */
  private Integer viewCount;

  /** 收藏次数 */
  private Integer collectCount;

  /** 是否已收藏 */
  private Boolean collected;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
