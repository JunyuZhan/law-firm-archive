package com.lawfirm.application.system.command;

import lombok.Data;

/** 创建字典项命令. */
@Data
public class CreateDictItemCommand {
  /** 字典类型ID. */
  private Long dictTypeId;

  /** 标签. */
  private String label;

  /** 值. */
  private String value;

  /** 描述. */
  private String description;

  /** 排序号. */
  private Integer sortOrder;

  /** CSS样式. */
  private String cssClass;
}
