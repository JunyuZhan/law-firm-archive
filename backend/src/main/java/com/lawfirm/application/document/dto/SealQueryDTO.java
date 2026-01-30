package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 印章查询DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class SealQueryDTO extends PageQuery {

  /** 印章名称（模糊搜索）. */
  private String name;

  /** 印章类型. */
  private String sealType;

  /** 保管人ID. */
  private Long keeperId;

  /** 状态. */
  private String status;
}
