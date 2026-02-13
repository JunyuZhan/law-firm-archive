package com.lawfirm.domain.client.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 客户关联企业实体。 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("crm_client_related_company")
public class ClientRelatedCompany extends BaseEntity {

  /** 客户ID */
  private Long clientId;

  /** 关联企业名称 */
  private String relatedCompanyName;

  /** 关联类型：PARENT-母公司, SUBSIDIARY-子公司, AFFILIATE-关联公司 */
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
