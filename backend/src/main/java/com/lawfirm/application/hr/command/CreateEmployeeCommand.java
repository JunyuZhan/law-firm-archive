package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建员工档案命令
 */
@Data
public class CreateEmployeeCommand {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private String employeeNo;
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
    private String emergencyContact;
    private String emergencyPhone;
    private String address;
    private String lawyerLicenseNo;
    private LocalDate licenseIssueDate;
    private LocalDate licenseExpireDate;
    private String licenseStatus;
    private String practiceArea;
    private Integer practiceYears;
    private String position;
    private String level;
    private LocalDate entryDate;
    private LocalDate probationEndDate;
    private String workStatus;
    private String remark;
}

