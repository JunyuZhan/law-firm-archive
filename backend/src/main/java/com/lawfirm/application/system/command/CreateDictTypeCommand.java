package com.lawfirm.application.system.command;

import lombok.Data;

/** 创建字典类型命令. */
@Data
public class CreateDictTypeCommand {
  /** 名称. */
  private String name;

  /** 编码. */
  private String code;

  /** 描述. */
  private String description;
}
