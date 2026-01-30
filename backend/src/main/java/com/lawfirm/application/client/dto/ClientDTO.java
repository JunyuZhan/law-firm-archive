package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户 DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientDTO extends BaseDTO {

  /** 客户编号 */
  private String clientNo;

  /** 客户名称 */
  private String name;

  /** 客户类型 */
  private String clientType;

  /** 客户类型名称 */
  private String clientTypeName;

  /** 统一社会信用代码 */
  private String creditCode;

  /** 身份证号 */
  private String idCard;

  /** 法定代表人 */
  private String legalRepresentative;

  /** 注册地址 */
  private String registeredAddress;

  /** 联系人 */
  private String contactPerson;

  /** 联系电话 */
  private String contactPhone;

  /** 联系邮箱 */
  private String contactEmail;

  /** 行业 */
  private String industry;

  /** 来源 */
  private String source;

  /** 客户级别 */
  private String level;

  /** 客户级别名称 */
  private String levelName;

  /** 客户类别 */
  private String category;

  /** 客户类别名称 */
  private String categoryName;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 创建人ID */
  private Long originatorId;

  /** 创建人名称 */
  private String originatorName;

  /** 负责律师ID */
  private Long responsibleLawyerId;

  /** 负责律师名称 */
  private String responsibleLawyerName;

  /** 首次合作日期 */
  private LocalDate firstCooperationDate;

  /** 备注 */
  private String remark;
}
