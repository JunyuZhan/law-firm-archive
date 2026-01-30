package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 系统参数配置实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

  /** 参数键. */
  private String configKey;

  /** 参数值. */
  private String configValue;

  /** 参数名称. */
  private String configName;

  /** 值类型. */
  private String configType;

  /** 描述. */
  private String description;

  /** 是否系统内置. */
  private Boolean isSystem;

  /** 类型：字符串. */
  public static final String TYPE_STRING = "STRING";

  /** 类型：数字. */
  public static final String TYPE_NUMBER = "NUMBER";

  /** 类型：布尔值. */
  public static final String TYPE_BOOLEAN = "BOOLEAN";

  /** 类型：JSON. */
  public static final String TYPE_JSON = "JSON";
}
