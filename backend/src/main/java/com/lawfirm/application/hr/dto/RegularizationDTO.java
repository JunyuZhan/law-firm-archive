package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 转正申请 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegularizationDTO extends BaseDTO {

    private Long employeeId;
    private Long userId;
    private String employeeName;
    private String applicationNo;
    private LocalDate probationStartDate;
    private LocalDate probationEndDate;
    private LocalDate applicationDate;
    private LocalDate expectedRegularDate;
    private String selfEvaluation;
    private String supervisorEvaluation;
    private String hrEvaluation;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private LocalDate approvedDate;
    private String comment;
}

