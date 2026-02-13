package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 更新关联企业命令 */
@Data
public class UpdateRelatedCompanyCommand {

  /** 关联企业ID */
  @NotNull(message = "关联企业ID不能为空")
  private Long id;

  /** 关联企业名称 */
  private String relatedCompanyName;

  /** 关联企业类型 */
  private String relatedCompanyType;

  /** 统一社会信用代码 */
  private String creditCode;

  /** 注册地址 */
  private String registeredAddress;

  /** 法定代表人 */
  private String legalRepresentative;

  /** 关系描述 */
  private String relationshipDescription;

  /** 备注 */
  private String remark;
}
