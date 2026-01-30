package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 用户查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserQueryDTO extends PageQuery {

  /** 用户名（模糊） */
  private String username;

  /** 真实姓名（模糊） */
  private String realName;

  /** 手机号 */
  private String phone;

  /** 部门ID */
  private Long departmentId;

  /** 状态 */
  private String status;

  /** 薪酬模式 */
  private String compensationType;
}
