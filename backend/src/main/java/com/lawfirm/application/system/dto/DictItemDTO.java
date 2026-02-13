package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 数据字典项DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictItemDTO extends BaseDTO {
  /** ID. */
  private Long id;

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

  /** 状态. */
  private String status;

  /** CSS样式. */
  private String cssClass;
}
