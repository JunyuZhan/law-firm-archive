package com.lawfirm.application.client.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 联系人 DTO */
@Data
public class ContactDTO {
  /** 联系人ID */
  private Long id;

  /** 客户ID */
  private Long clientId;

  /** 联系人姓名 */
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

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 更新时间 */
  private LocalDateTime updatedAt;
}
