package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 员工档案 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDTO extends BaseDTO {

    private Long userId;
    private String employeeNo;
    private String realName;
    private String email;
    private String phone;
    private Long departmentId;
    private String departmentName;

    // 基本信息
    private String gender;
    private LocalDate birthDate;
    private String idCard;
    private String nationality;
    private String nativePlace;
    private String politicalStatus;
    private String education;
    private String major;
    private String graduationSchool;
    private LocalDate graduationDate;

    // 联系信息
    private String emergencyContact;
    private String emergencyPhone;
    private String address;

    // 执业信息
    private String lawyerLicenseNo;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpireDate;
    private String licenseStatus;
    private String practiceArea;
    private Integer practiceYears;

    // 工作信息
    private String position;
    private String level;
    private LocalDate entryDate;
    private LocalDate probationEndDate;
    private LocalDate regularDate;
    private LocalDate resignationDate;
    private String resignationReason;
    private String workStatus;
    private String workStatusName;

    private String remark;
}

