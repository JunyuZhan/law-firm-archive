package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 创建联系记录命令 */
@Data
public class CreateContactRecordCommand {

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long clientId;

  /** 联系人ID（可选） */
  private Long contactId;

  /** 联系人姓名（如果未指定contactId） */
  private String contactPerson;

  /** 联系方式（PHONE-电话, EMAIL-邮件, MEETING-会面, VISIT-拜访, OTHER-其他） */
  @NotBlank(message = "联系方式不能为空")
  private String contactMethod;

  /** 联系时间 */
  @NotNull(message = "联系时间不能为空")
  private LocalDateTime contactDate;

  /** 联系时长（分钟） */
  private Integer contactDuration;

  /** 联系地点 */
  private String contactLocation;

  /** 联系内容 */
  private String contactContent;

  /** 联系结果 */
  private String contactResult;

  /** 下次跟进日期 */
  private LocalDate nextFollowUpDate;

  /** 是否设置提醒 */
  private Boolean followUpReminder;
}
