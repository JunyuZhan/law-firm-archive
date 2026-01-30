package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDTO extends BaseDTO {

  /** 用户名. */
  private String username;

  /** 真实姓名. */
  private String realName;

  /** 邮箱. */
  private String email;

  /** 电话. */
  private String phone;

  /** 头像URL. */
  private String avatarUrl;

  /** 部门ID. */
  private Long departmentId;

  /** 部门名称. */
  private String departmentName;

  /** 职位. */
  private String position;

  /** 工号. */
  private String employeeNo;

  /** 律师执业证号. */
  private String lawyerLicenseNo;

  /** 入职日期. */
  private LocalDate joinDate;

  /** 薪酬类型. */
  private String compensationType;

  /** 薪酬类型名称. */
  private String compensationTypeName;

  /** 是否可为案源人. */
  private Boolean canBeOriginator;

  /** 状态. */
  private String status;

  /** 角色ID列表. */
  private List<Long> roleIds;

  /** 角色编码列表. */
  private List<String> roleCodes;

  /** 权限编码列表. */
  private List<String> permissions;
}
