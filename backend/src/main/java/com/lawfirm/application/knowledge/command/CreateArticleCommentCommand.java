package com.lawfirm.application.knowledge.command;

import lombok.Data;

/**
 * 创建文章评论命令（M10-022）
 */
@Data
public class CreateArticleCommentCommand {
    private Long articleId;
    private Long parentId; // 父评论ID，用于回复
    private String content;
}

