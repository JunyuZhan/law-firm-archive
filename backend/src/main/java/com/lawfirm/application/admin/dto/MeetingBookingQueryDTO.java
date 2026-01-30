package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/** 会议预约查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingBookingQueryDTO extends PageQuery {
  /** 会议室ID */
  private Long roomId;

  /** 组织者ID */
  private Long organizerId;

  /** 状态 */
  private String status;

  /** 开始时间 */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startTime;

  /** 结束时间 */
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endTime;
}
