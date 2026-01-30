package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 会议预约DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingBookingDTO extends BaseDTO {
  /** 预约ID */
  private Long id;

  /** 预约编号 */
  private String bookingNo;

  /** 会议室ID */
  private Long roomId;

  /** 会议室名称 */
  private String roomName;

  /** 会议标题 */
  private String title;

  /** 组织者ID */
  private Long organizerId;

  /** 组织者名称 */
  private String organizerName;

  /** 开始时间 */
  private LocalDateTime startTime;

  /** 结束时间 */
  private LocalDateTime endTime;

  /** 参会人员ID列表 */
  private List<Long> attendeeIds;

  /** 参会人员 */
  private String attendees;

  /** 会议描述 */
  private String description;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
