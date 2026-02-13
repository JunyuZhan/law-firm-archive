package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统配置DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigDTO extends BaseDTO {
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

  /** 是否系统内置. */
  private Boolean isSystem;
}
