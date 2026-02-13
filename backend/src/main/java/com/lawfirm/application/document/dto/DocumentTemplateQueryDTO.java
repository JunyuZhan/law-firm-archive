package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档模板查询DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentTemplateQueryDTO extends PageQuery {

  /** 模板名称（模糊搜索）. */
  private String name;

  /** 分类ID. */
  private Long categoryId;

  /** 模板类型. */
  private String templateType;

  /** 状态. */
  private String status;
}
