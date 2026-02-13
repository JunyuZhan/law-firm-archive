package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Data;

/** 创建会议记录命令（M8-023） */
@Data
public class CreateMeetingRecordCommand {
  /** 关联预约ID */
  private Long bookingId;

  /** 会议室ID */
  @NotNull(message = "会议室ID不能为空")
  private Long roomId;

  /** 会议主题 */
  @NotNull(message = "会议主题不能为空")
  private String title;

  /** 会议日期 */
  @NotNull(message = "会议日期不能为空")
  private LocalDate meetingDate;

  /** 开始时间 */
  @NotNull(message = "开始时间不能为空")
  private LocalTime startTime;

  /** 结束时间 */
  @NotNull(message = "结束时间不能为空")
  private LocalTime endTime;

  /** 参会人员ID列表 */
  private List<Long> attendeeIds;

  /** 会议内容 */
  private String content;

  /** 会议决议 */
  private String decisions;

  /** 行动项（JSON格式） */
  private String actionItems;

  /** 附件URL */
  private String attachmentUrl;
}
