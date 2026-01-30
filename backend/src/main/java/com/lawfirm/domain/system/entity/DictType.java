package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 数据字典类型实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class DictType extends BaseEntity {

  /** 字典名称. */
  private String name;

  /** 字典编码. */
  private String code;

  /** 描述. */
  private String description;

  /** 状态. */
  private String status;

  /** 是否系统内置. */
  private Boolean isSystem;

  /** 状态：启用. */
  public static final String STATUS_ENABLED = "ENABLED";

  /** 状态：禁用. */
  public static final String STATUS_DISABLED = "DISABLED";
}
