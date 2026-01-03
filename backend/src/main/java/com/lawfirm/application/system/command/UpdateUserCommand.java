package com.lawfirm.application.system.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新用户命令
 */
public class UpdateUserCommand {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String realName;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private Long departmentId;

    private String position;

    private String employeeNo;

    private String lawyerLicenseNo;

    private LocalDate joinDate;

    /**
     * 薪酬模式：COMMISSION, SALARIED, HYBRID
     */
    private String compensationType;

    private Boolean canBeOriginator;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    // Getters
    public Long getId() { return id; }
    public String getRealName() { return realName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Long getDepartmentId() { return departmentId; }
    public String getPosition() { return position; }
    public String getEmployeeNo() { return employeeNo; }
    public String getLawyerLicenseNo() { return lawyerLicenseNo; }
    public LocalDate getJoinDate() { return joinDate; }
    public String getCompensationType() { return compensationType; }
    public Boolean getCanBeOriginator() { return canBeOriginator; }
    public List<Long> getRoleIds() { return roleIds; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setRealName(String realName) { this.realName = realName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setPosition(String position) { this.position = position; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public void setLawyerLicenseNo(String lawyerLicenseNo) { this.lawyerLicenseNo = lawyerLicenseNo; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public void setCompensationType(String compensationType) { this.compensationType = compensationType; }
    public void setCanBeOriginator(Boolean canBeOriginator) { this.canBeOriginator = canBeOriginator; }
    public void setRoleIds(List<Long> roleIds) { this.roleIds = roleIds; }
}
