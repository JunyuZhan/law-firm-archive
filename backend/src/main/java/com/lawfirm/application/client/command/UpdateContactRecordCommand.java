package com.lawfirm.application.client.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 更新联系记录命令 */
@Data
public class UpdateContactRecordCommand {

  /** 联系记录ID */
  @NotNull(message = "联系记录ID不能为空")
  private Long id;

  /** 联系人ID */
  private Long contactId;

  /** 联系人姓名 */
  private String contactPerson;

  /** 联系方式 */
  private String contactMethod;

  /** 联系时间 */
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
