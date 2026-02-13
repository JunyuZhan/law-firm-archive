package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 法规分类DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class LawCategoryDTO extends BaseDTO {

  /** ID */
  private Long id;

  /** 名称 */
  private String name;

  /** 父级ID */
  private Long parentId;

  /** 层级 */
  private Integer level;

  /** 排序 */
  private Integer sortOrder;

  /** 描述 */
  private String description;

  /** 子分类 */
  private List<LawCategoryDTO> children;
}
