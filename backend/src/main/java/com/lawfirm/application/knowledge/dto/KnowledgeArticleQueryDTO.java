package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeArticleQueryDTO extends PageQuery {
    private Long authorId;
    private String status;
    private String category;
    private String keyword;
}
