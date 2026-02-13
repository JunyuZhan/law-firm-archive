package com.lawfirm.application.document.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建文档模板命令 */
@Data
public class CreateDocumentTemplateCommand {

  /** 模板名称. */
  @NotBlank(message = "模板名称不能为空")
  private String name;

  /** 分类ID. */
  private Long categoryId;

  /** 模板类型. */
  private String templateType;

  /** 文件名. */
  @NotBlank(message = "文件名不能为空")
  private String fileName;

  /** 文件路径. */
  @NotBlank(message = "文件路径不能为空")
  private String filePath;

  /** 文件大小. */
  private Long fileSize;

  /** 变量定义（逗号分隔的变量名）. */
  private String variables;

  /** 描述. */
  private String description;
}
