package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建劳动合同命令
 */
@Data
public class CreateContractCommand {

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    private String contractNo;
    
    @NotNull(message = "合同类型不能为空")
    private String contractType;

    @NotNull(message = "合同开始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;
    private Integer probationMonths;
    private LocalDate probationEndDate;
    private BigDecimal baseSalary;
    private BigDecimal performanceBonus;
    private BigDecimal otherAllowance;
    private LocalDate signDate;
    private String contractFileUrl;
    private String remark;
}

