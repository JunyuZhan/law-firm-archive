package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户关联企业 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientRelatedCompanyDTO extends BaseDTO {

  /** 客户ID */
  private Long clientId;

  /** 关联企业名称 */
  private String relatedCompanyName;

  /** 关联企业类型 */
  private String relatedCompanyType;

  /** 关联企业类型名称 */
  private String relatedCompanyTypeName;

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
