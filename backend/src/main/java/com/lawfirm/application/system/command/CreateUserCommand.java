package com.lawfirm.application.system.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建用户命令
 */
@Data
@Builder
public class CreateUserCommand {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @NotBlank(message = "姓名不能为空")
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
}

