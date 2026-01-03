package com.lawfirm.application.hr.command;

import lombok.Data;

import java.time.LocalDate;

/**
 * 更新员工档案命令
 */
@Data
public class UpdateEmployeeCommand {

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
    private LocalDate regularDate;
    private String workStatus;
    private String remark;
}

