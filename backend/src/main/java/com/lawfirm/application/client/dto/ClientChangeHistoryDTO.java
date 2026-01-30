package com.lawfirm.application.client.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 企业变更历史DTO（M2-014） */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientChangeHistoryDTO extends BaseDTO {
  /** 客户ID */
  private Long clientId;

  /** 客户名称 */
  private String clientName;

  /** 变更类型 */
  private String changeType;

  /** 变更类型名称 */
  private String changeTypeName;

  /** 变更日期 */
  private LocalDate changeDate;

  /** 变更前值 */
  private String beforeValue;

  /** 变更后值 */
  private String afterValue;

  /** 变更描述 */
  private String changeDescription;

  /** 登记机关 */
  private String registrationAuthority;

  /** 登记编号 */
  private String registrationNumber;

  /** 附件URL */
  private String attachmentUrl;
}
