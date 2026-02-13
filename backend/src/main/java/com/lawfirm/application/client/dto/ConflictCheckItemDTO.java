package com.lawfirm.application.client.dto;

import lombok.Data;

/** 利冲检查项 DTO */
@Data
public class ConflictCheckItemDTO {

  /** 检查项ID */
  private Long id;

  /** 检查ID */
  private Long checkId;

  /** 当事人名称 */
  private String partyName;

  /** 当事人类型 */
  private String partyType;

  /** 当事人类型名称 */
  private String partyTypeName;

  /** 证件号码 */
  private String idNumber;

  /** 是否存在冲突 */
  private Boolean hasConflict;

  /** 冲突详情 */
  private String conflictDetail;

  /** 关联案件ID */
  private Long relatedMatterId;

  /** 关联案件名称 */
  private String relatedMatterName;

  /** 关联客户ID */
  private Long relatedClientId;

  /** 关联客户名称 */
  private String relatedClientName;
}
