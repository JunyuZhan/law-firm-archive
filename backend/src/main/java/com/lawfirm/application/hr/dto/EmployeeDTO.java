package com.lawfirm.application.hr.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 员工档案 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class EmployeeDTO extends BaseDTO {

  /** 用户ID */
  private Long userId;

  /** 工号 */
  private String employeeNo;

  /** 真实姓名 */
  private String realName;

  /** 邮箱 */
  private String email;

  /** 电话 */
  private String phone;

  /** 部门ID */
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  // 基本信息
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

  // 联系信息
  /** 紧急联系人 */
  private String emergencyContact;

  /** 紧急联系电话 */
  private String emergencyPhone;

  /** 地址 */
  private String address;

  // 执业信息
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

  // 工作信息
  /** 职位 */
  private String position;

  /** 级别 */
  private String level;

  /** 入职日期 */
  private LocalDate entryDate;

  /** 试用期结束日期 */
  private LocalDate probationEndDate;

  /** 转正日期 */
  private LocalDate regularDate;

  /** 离职日期 */
  private LocalDate resignationDate;

  /** 离职原因 */
  private String resignationReason;

  /** 工作状态 */
  private String workStatus;

  /** 工作状态名称 */
  private String workStatusName;

  /** 备注 */
  private String remark;
}
