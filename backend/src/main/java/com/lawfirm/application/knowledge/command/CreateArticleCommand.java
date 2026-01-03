package com.lawfirm.application.knowledge.command;

import lombok.Data;

/**
 * 创建文章命令
 */
@Data
public class CreateArticleCommand {
    private String title;
    private String category;
    private String content;
    private String summary;
    private String tags;
}
