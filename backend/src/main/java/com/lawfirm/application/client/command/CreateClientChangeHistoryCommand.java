package com.lawfirm.application.client.command;

import java.time.LocalDate;
import lombok.Data;

/** 创建企业变更历史命令（M2-014） */
@Data
public class CreateClientChangeHistoryCommand {
  /** 客户ID */
  private Long clientId;

  /** 变更类型 */
  private String changeType;

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
