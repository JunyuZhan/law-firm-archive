package com.lawfirm.application.knowledge.command;

import java.time.LocalDate;
import lombok.Data;

/** 创建案例命令 */
@Data
public class CreateCaseLibraryCommand {
  /** 标题 */
  private String title;

  /** 类别ID */
  private Long categoryId;

  /** 案号 */
  private String caseNumber;

  /** 法院名称 */
  private String courtName;

  /** 审理日期 */
  private LocalDate judgeDate;

  /** 案件类型 */
  private String caseType;

  /** 案由 */
  private String causeOfAction;

  /** 审理程序 */
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

  /** 关联案件ID */
  private Long matterId;

  /** 附件URL */
  private String attachmentUrl;
}
