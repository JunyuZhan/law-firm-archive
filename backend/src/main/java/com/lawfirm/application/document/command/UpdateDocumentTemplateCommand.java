package com.lawfirm.application.document.command;

import lombok.Data;

/** 更新文档模板命令 */
@Data
public class UpdateDocumentTemplateCommand {

  /** 模板名称. */
  private String name;

  /** 分类ID. */
  private Long categoryId;

  /** 模板类型. */
  private String templateType;

  /** 描述. */
  private String description;

  /** 模板内容. */
  private String content;

  /** 状态. */
  private String status;
}
