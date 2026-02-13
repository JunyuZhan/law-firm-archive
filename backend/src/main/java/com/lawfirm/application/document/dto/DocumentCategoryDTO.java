package com.lawfirm.application.document.dto;

import com.lawfirm.common.base.BaseDTO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文档分类DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DocumentCategoryDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 分类名称. */
  private String name;

  /** 父分类ID. */
  private Long parentId;

  /** 父分类名称. */
  private String parentName;

  /** 排序顺序. */
  private Integer sortOrder;

  /** 描述. */
  private String description;

  /** 子分类（树形结构用）. */
  private List<DocumentCategoryDTO> children;
}
