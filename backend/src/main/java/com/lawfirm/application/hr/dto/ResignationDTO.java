package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 离职申请 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResignationDTO extends BaseDTO {

    private Long employeeId;
    private Long userId;
    private String employeeName;
    private String applicationNo;
    private String resignationType;
    private String resignationTypeName;
    private LocalDate resignationDate;
    private LocalDate lastWorkDate;
    private String reason;
    private Long handoverPersonId;
    private String handoverPersonName;
    private String handoverStatus;
    private String handoverStatusName;
    private String handoverNote;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private LocalDate approvedDate;
    private String comment;
}

