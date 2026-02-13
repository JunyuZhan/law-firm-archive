package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 日程实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("schedule")
public class Schedule extends BaseEntity {

  /** 案件ID */
  private Long matterId;

  /** 用户ID */
  private Long userId;

  /** 日程标题 */
  private String title;

  /** 描述 */
  private String description;

  /** 地点 */
  private String location;

  /** 日程类型 */
  private String scheduleType;

  /** 开始时间 */
  private LocalDateTime startTime;

  /** 结束时间 */
  private LocalDateTime endTime;

  /** 是否全天 */
  @lombok.Builder.Default private Boolean allDay = false;

  /** 提前提醒分钟数 */
  private Integer reminderMinutes;

  /** 提醒是否已发送 */
  @lombok.Builder.Default private Boolean reminderSent = false;

  /** 重复规则 */
  private String recurrenceRule;

  /** 状态 */
  @lombok.Builder.Default private String status = "ACTIVE";
}
