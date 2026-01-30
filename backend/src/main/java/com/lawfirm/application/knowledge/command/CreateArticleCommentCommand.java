package com.lawfirm.application.knowledge.command;

import lombok.Data;

/** 创建文章评论命令（M10-022） */
@Data
public class CreateArticleCommentCommand {
  /** 文章ID */
  private Long articleId;

  /** 父评论ID，用于回复 */
  private Long parentId;

  /** 评论内容 */
  private String content;
}
