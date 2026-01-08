package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工资表查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollSheetQueryDTO extends PageQuery {

    private Integer payrollYear;
    private Integer payrollMonth;
    private String status;
    private String payrollNo;
}

