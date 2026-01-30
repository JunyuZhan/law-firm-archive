package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 案例查询参数. */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseLibraryQueryDTO extends PageQuery {

  /** 分类ID */
  private Long categoryId;

  /** 来源 */
  private String source;

  /** 案件类型 */
  private String caseType;

  /** 关键词 */
  private String keyword;
}
