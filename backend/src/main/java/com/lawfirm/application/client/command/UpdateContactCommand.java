package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 更新联系人命令 */
@Data
public class UpdateContactCommand {

  /** 联系人姓名 */
  @NotBlank(message = "联系人姓名不能为空")
  private String contactName;

  /** 职位 */
  private String position;

  /** 部门 */
  private String department;

  /** 手机号 */
  private String mobilePhone;

  /** 办公电话 */
  private String officePhone;

  /** 邮箱 */
  private String email;

  /** 微信 */
  private String wechat;

  /** 是否主要联系人 */
  private Boolean isPrimary;

  /** 关系备注 */
  private String relationshipNote;
}
