package com.lawfirm.application.hr.command;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新劳动合同命令
 */
@Data
public class UpdateContractCommand {

    private String contractNo;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer probationMonths;
    private LocalDate probationEndDate;
    private BigDecimal baseSalary;
    private BigDecimal performanceBonus;
    private BigDecimal otherAllowance;
    private String status;
    private LocalDate signDate;
    private LocalDate expireDate;
    private String contractFileUrl;
    private String remark;
}

