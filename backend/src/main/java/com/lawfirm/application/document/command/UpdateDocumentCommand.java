package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新文档命令
 */
@Data
public class UpdateDocumentCommand {

    @NotNull(message = "文档ID不能为空")
    private Long id;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 安全级别
     */
    private String securityLevel;

    /**
     * 文档阶段
     */
    private String stage;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 描述
     */
    private String description;
}
