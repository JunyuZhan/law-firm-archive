package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 晋升申请查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromotionQueryDTO extends PageQuery {
    private String keyword;
    private String status;
    private Long employeeId;
    private Long departmentId;
}
