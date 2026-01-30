package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 日程DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 案件ID. */
  private Long matterId;

  /** 案件名称. */
  private String matterName;

  /** 用户ID. */
  private Long userId;

  /** 用户姓名. */
  private String userName;

  /** 日程标题. */
  private String title;

  /** 日程描述. */
  private String description;

  /** 地点. */
  private String location;

  /** 日程类型. */
  private String scheduleType;

  /** 日程类型名称. */
  private String scheduleTypeName;

  /** 开始时间. */
  private LocalDateTime startTime;

  /** 结束时间. */
  private LocalDateTime endTime;

  /** 是否全天. */
  private Boolean allDay;

  /** 提醒时间（分钟）. */
  private Integer reminderMinutes;

  /** 是否已发送提醒. */
  private Boolean reminderSent;

  /** 重复规则. */
  private String recurrenceRule;

  /** 状态. */
  private String status;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
