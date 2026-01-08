package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建文档模板命令
 */
@Data
public class CreateDocumentTemplateCommand {

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 模板类型
     */
    private String templateType;

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件路径
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 变量定义（变量名列表）
     */
    private List<String> variables;

    /**
     * 描述
     */
    private String description;
}
