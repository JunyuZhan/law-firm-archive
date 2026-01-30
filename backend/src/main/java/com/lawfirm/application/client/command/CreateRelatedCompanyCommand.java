package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建关联企业命令 */
@Data
public class CreateRelatedCompanyCommand {

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long clientId;

  /** 关联企业名称 */
  @NotBlank(message = "关联企业名称不能为空")
  private String relatedCompanyName;

  /** 关联类型（PARENT-母公司, SUBSIDIARY-子公司, AFFILIATE-关联公司） */
  @NotBlank(message = "关联类型不能为空")
  private String relatedCompanyType;

  /** 统一社会信用代码 */
  private String creditCode;

  /** 注册地址 */
  private String registeredAddress;

  /** 法定代表人 */
  private String legalRepresentative;

  /** 关联关系描述 */
  private String relationshipDescription;

  /** 备注 */
  private String remark;
}
