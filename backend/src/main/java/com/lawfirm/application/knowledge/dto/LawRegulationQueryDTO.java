package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 法规查询参数. */
@Data
@EqualsAndHashCode(callSuper = true)
public class LawRegulationQueryDTO extends PageQuery {

  /** 分类ID */
  private Long categoryId;

  /** 状态 */
  private String status;

  /** 关键词 */
  private String keyword;
}
