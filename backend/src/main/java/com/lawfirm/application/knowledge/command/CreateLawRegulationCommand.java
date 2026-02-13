package com.lawfirm.application.knowledge.command;

import java.time.LocalDate;
import lombok.Data;

/** 创建法规命令 */
@Data
public class CreateLawRegulationCommand {
  /** 标题 */
  private String title;

  /** 类别ID */
  private Long categoryId;

  /** 文号 */
  private String docNumber;

  /** 发布机关 */
  private String issuingAuthority;

  /** 发布日期 */
  private LocalDate issueDate;

  /** 生效日期 */
  private LocalDate effectiveDate;

  /** 失效日期 */
  private LocalDate expiryDate;

  /** 状态 */
  private String status;

  /** 内容 */
  private String content;

  /** 摘要 */
  private String summary;

  /** 关键词 */
  private String keywords;

  /** 来源 */
  private String source;

  /** 附件URL */
  private String attachmentUrl;
}
