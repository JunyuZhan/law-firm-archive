package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建文档命令
 */
@Data
public class CreateDocumentCommand {

    /**
     * 文档标题
     */
    @NotBlank(message = "文档标题不能为空")
    private String title;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 关联案件ID
     */
    private Long matterId;

    /**
     * 原始文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件存储路径
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

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
