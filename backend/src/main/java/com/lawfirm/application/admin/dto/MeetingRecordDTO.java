package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 会议记录DTO（M8-023） */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingRecordDTO extends BaseDTO {
  /** 记录ID */
  private Long id;

  /** 记录编号 */
  private String recordNo;

  /** 预约ID */
  private Long bookingId;

  /** 会议室ID */
  private Long roomId;

  /** 会议室名称 */
  private String roomName;

  /** 会议标题 */
  private String title;

  /** 会议日期 */
  private LocalDate meetingDate;

  /** 开始时间 */
  private LocalTime startTime;

  /** 结束时间 */
  private LocalTime endTime;

  /** 组织者ID */
  private Long organizerId;

  /** 组织者名称 */
  private String organizerName;

  /** 参会人员 */
  private String attendees;

  /** 会议内容 */
  private String content;

  /** 会议决议 */
  private String decisions;

  /** 行动项 */
  private String actionItems;

  /** 附件URL */
  private String attachmentUrl;
}
