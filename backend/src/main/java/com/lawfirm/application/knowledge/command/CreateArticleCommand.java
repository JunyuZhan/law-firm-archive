package com.lawfirm.application.knowledge.command;

import lombok.Data;

/** 创建文章命令 */
@Data
public class CreateArticleCommand {
  /** 标题 */
  private String title;

  /** 类别 */
  private String category;

  /** 内容 */
  private String content;

  /** 摘要 */
  private String summary;

  /** 标签 */
  private String tags;
}
