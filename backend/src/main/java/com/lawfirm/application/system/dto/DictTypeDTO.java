package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 数据字典类型DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictTypeDTO extends BaseDTO {
  /** ID. */
  private Long id;

  /** 名称. */
  private String name;

  /** 编码. */
  private String code;

  /** 描述. */
  private String description;

  /** 状态. */
  private String status;

  /** 是否系统内置. */
  private Boolean isSystem;

  /** 字典项列表. */
  private List<DictItemDTO> items;
}
