package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工资扣减项 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PayrollDeductionDTO extends BaseDTO {

    private Long payrollItemId;
    private String deductionType;
    private String deductionTypeName;
    private BigDecimal amount;
    private String remark;
    private String sourceType;
    private String sourceTypeName;
}

