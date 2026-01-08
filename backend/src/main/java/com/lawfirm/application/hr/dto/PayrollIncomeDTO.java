package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工资收入项 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollIncomeDTO extends BaseDTO {

    private Long payrollItemId;
    private String incomeType;
    private String incomeTypeName;
    private BigDecimal amount;
    private String remark;
    private String sourceType;
    private String sourceTypeName;
    private Long sourceId;
}

