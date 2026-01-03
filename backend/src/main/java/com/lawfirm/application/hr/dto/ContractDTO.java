package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 劳动合同 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContractDTO extends BaseDTO {

    private Long employeeId;
    private Long userId;
    private String employeeName;
    private String contractNo;
    private String contractType;
    private String contractTypeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer probationMonths;
    private LocalDate probationEndDate;
    private BigDecimal baseSalary;
    private BigDecimal performanceBonus;
    private BigDecimal otherAllowance;
    private String status;
    private String statusName;
    private LocalDate signDate;
    private LocalDate expireDate;
    private Integer renewCount;
    private String contractFileUrl;
    private String remark;
}

