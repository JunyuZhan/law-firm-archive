package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 创建员工档案命令 */
@Data
public class CreateEmployeeCommand {

  /** 用户ID */
  @NotNull(message = "用户ID不能为空")
  private Long userId;

  /** 工号 */
  private String employeeNo;

  /** 性别 */
  private String gender;

  /** 出生日期 */
  private LocalDate birthDate;

  /** 身份证号 */
  private String idCard;

  /** 国籍 */
  private String nationality;

  /** 籍贯 */
  private String nativePlace;

  /** 政治面貌 */
  private String politicalStatus;

  /** 学历 */
  private String education;

  /** 专业 */
  private String major;

  /** 毕业院校 */
  private String graduationSchool;

  /** 毕业日期 */
  private LocalDate graduationDate;

  /** 紧急联系人 */
  private String emergencyContact;

  /** 紧急联系电话 */
  private String emergencyPhone;

  /** 地址 */
  private String address;

  /** 律师执业证号 */
  private String lawyerLicenseNo;

  /** 执业证发证日期 */
  private LocalDate licenseIssueDate;

  /** 执业证到期日期 */
  private LocalDate licenseExpireDate;

  /** 执业证状态 */
  private String licenseStatus;

  /** 执业领域 */
  private String practiceArea;

  /** 执业年限 */
  private Integer practiceYears;

  /** 职位 */
  private String position;

  /** 级别 */
  private String level;

  /** 入职日期 */
  private LocalDate entryDate;

  /** 试用期结束日期 */
  private LocalDate probationEndDate;

  /** 工作状态 */
  private String workStatus;

  /** 备注 */
  private String remark;
}
