package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 会议记录实体（M8-023） */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_record")
public class MeetingRecord extends BaseEntity {

  /** 记录编号 */
  private String recordNo;

  /** 关联预约ID */
  private Long bookingId;

  /** 会议室ID */
  private Long roomId;

  /** 会议主题 */
  private String title;

  /** 会议日期 */
  private LocalDate meetingDate;

  /** 开始时间 */
  private LocalTime startTime;

  /** 结束时间 */
  private LocalTime endTime;

  /** 组织者ID */
  private Long organizerId;

  /** 参会人员(JSON数组) */
  private String attendees;

  /** 会议内容 */
  private String content;

  /** 会议决议 */
  private String decisions;

  /** 行动项(JSON格式) */
  private String actionItems;

  /** 附件URL */
  private String attachmentUrl;
}
