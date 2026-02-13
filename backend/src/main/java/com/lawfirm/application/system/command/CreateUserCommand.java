package com.lawfirm.application.system.command;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/** 创建用户命令. */
@Data
@Builder
public class CreateUserCommand {

  /** 用户名. */
  @NotBlank(message = "用户名不能为空")
  @Size(min = 3, max = 50, message = "用户名长度3-50位")
  private String username;

  /** 密码. */
  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 20, message = "密码长度6-20位")
  private String password;

  /** 真实姓名. */
  @NotBlank(message = "姓名不能为空")
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
}
