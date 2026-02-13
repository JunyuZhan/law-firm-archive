package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 会议预约实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_booking")
public class MeetingBooking extends BaseEntity {

  /** 预约编号. */
  private String bookingNo;

  /** 会议室ID. */
  private Long roomId;

  /** 会议主题. */
  private String title;

  /** 组织者ID. */
  private Long organizerId;

  /** 开始时间. */
  private LocalDateTime startTime;

  /** 结束时间. */
  private LocalDateTime endTime;

  /** 参会人员(JSON数组). */
  private String attendees;

  /** 会议描述. */
  private String description;

  /** 状态: BOOKED已预约/IN_PROGRESS进行中/COMPLETED已完成/CANCELLED已取消. */
  private String status;

  /** 是否已发送提醒. */
  private Boolean reminderSent;

  // 状态常量
  /** 状态：已预约. */
  public static final String STATUS_BOOKED = "BOOKED";

  /** 状态：进行中. */
  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

  /** 状态：已完成. */
  public static final String STATUS_COMPLETED = "COMPLETED";

  /** 状态：已取消. */
  public static final String STATUS_CANCELLED = "CANCELLED";
}
