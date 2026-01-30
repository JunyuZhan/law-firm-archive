package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 期限提醒 DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeadlineDTO extends BaseDTO {

  /** 案件ID. */
  private Long matterId;

  /** 案件编号. */
  private String matterNo;

  /** 案件名称. */
  private String matterName;

  /** 期限类型. */
  private String deadlineType;

  /** 期限类型名称. */
  private String deadlineTypeName;

  /** 期限名称. */
  private String deadlineName;

  /** 基准日期. */
  private LocalDate baseDate;

  /** 期限日期. */
  private LocalDate deadlineDate;

  /** 提醒天数. */
  private Integer reminderDays;

  /** 是否已发送提醒. */
  private Boolean reminderSent;

  /** 提醒发送时间. */
  private LocalDateTime reminderSentAt;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 完成时间. */
  private LocalDateTime completedAt;

  /** 完成人ID. */
  private Long completedBy;

  /** 完成人姓名. */
  private String completedByName;

  /** 描述. */
  private String description;

  /** 剩余天数（负数表示已过期）. */
  private Long daysRemaining;
}
