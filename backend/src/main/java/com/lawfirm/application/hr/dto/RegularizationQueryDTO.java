package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 转正申请查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegularizationQueryDTO extends PageQuery {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 状态
     */
    private String status;
}

