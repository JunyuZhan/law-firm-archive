package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 文章查询参数. */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeArticleQueryDTO extends PageQuery {

  /** 作者ID */
  private Long authorId;

  /** 状态 */
  private String status;

  /** 分类 */
  private String category;

  /** 关键词 */
  private String keyword;
}
