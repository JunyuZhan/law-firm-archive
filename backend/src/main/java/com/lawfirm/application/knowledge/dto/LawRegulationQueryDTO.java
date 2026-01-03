package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 法规查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LawRegulationQueryDTO extends PageQuery {
    private Long categoryId;
    private String status;
    private String keyword;
}
