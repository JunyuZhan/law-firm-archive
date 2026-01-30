package com.lawfirm.application.system.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** 更新用户命令. */
public class UpdateUserCommand {

  /** ID. */
  @NotNull(message = "用户ID不能为空")
  private Long id;

  /** 真实姓名. */
  private String realName;

  /** 邮箱. */
  @Email(message = "邮箱格式不正确")
  private String email;

  /** 电话. */
  private String phone;

  /** 部门ID. */
  private Long departmentId;

  /** 职位. */
  private String position;

  /** 工号. */
  private String employeeNo;

  /** 律师执业证号. */
  private String lawyerLicenseNo;

  /** 入职日期. */
  private LocalDate joinDate;

  /** 薪酬模式：COMMISSION, SALARIED, HYBRID. */
  private String compensationType;

  /** 是否可为案源人. */
  private Boolean canBeOriginator;

  /** 角色ID列表. */
  private List<Long> roleIds;

  /** 角色变更原因（可选）. */
  private String roleChangeReason;

  // Getters
  public Long getId() {
    return id;
  }

  public String getRealName() {
    return realName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public String getPosition() {
    return position;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public String getLawyerLicenseNo() {
    return lawyerLicenseNo;
  }

  public LocalDate getJoinDate() {
    return joinDate;
  }

  public String getCompensationType() {
    return compensationType;
  }

  public Boolean getCanBeOriginator() {
    return canBeOriginator;
  }

  public List<Long> getRoleIds() {
    return roleIds;
  }

  public String getRoleChangeReason() {
    return roleChangeReason;
  }

  // Setters
  public void setId(final Long id) {
    this.id = id;
  }

  public void setRealName(final String realName) {
    this.realName = realName;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public void setPhone(final String phone) {
    this.phone = phone;
  }

  public void setDepartmentId(final Long departmentId) {
    this.departmentId = departmentId;
  }

  public void setPosition(final String position) {
    this.position = position;
  }

  public void setEmployeeNo(final String employeeNo) {
    this.employeeNo = employeeNo;
  }

  public void setLawyerLicenseNo(final String lawyerLicenseNo) {
    this.lawyerLicenseNo = lawyerLicenseNo;
  }

  public void setJoinDate(final LocalDate joinDate) {
    this.joinDate = joinDate;
  }

  public void setCompensationType(final String compensationType) {
    this.compensationType = compensationType;
  }

  public void setCanBeOriginator(final Boolean canBeOriginator) {
    this.canBeOriginator = canBeOriginator;
  }

  public void setRoleIds(final List<Long> roleIds) {
    this.roleIds = roleIds;
  }

  public void setRoleChangeReason(final String roleChangeReason) {
    this.roleChangeReason = roleChangeReason;
  }
}
