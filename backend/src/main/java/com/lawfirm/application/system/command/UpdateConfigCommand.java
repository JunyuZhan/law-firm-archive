package com.lawfirm.application.system.command;

import lombok.Data;

/** 更新配置命令. */
@Data
public class UpdateConfigCommand {
  /** ID. */
  private Long id;

  /** 配置键. */
  private String configKey;

  /** 配置值. */
  private String configValue;

  /** 配置名称. */
  private String configName;

  /** 配置类型. */
  private String configType;

  /** 描述. */
  private String description;
}
