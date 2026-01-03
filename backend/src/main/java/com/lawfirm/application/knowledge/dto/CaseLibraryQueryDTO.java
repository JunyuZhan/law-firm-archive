package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 案例查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CaseLibraryQueryDTO extends PageQuery {
    private Long categoryId;
    private String source;
    private String caseType;
    private String keyword;
}
