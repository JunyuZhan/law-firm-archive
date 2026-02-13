package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 请假申请查询参数 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveApplicationQueryDTO extends PageQuery {
  /** 用户ID */
  private Long userId;

  /** 请假类型ID */
  private Long leaveTypeId;

  /** 状态 */
  private String status;

  /** 开始时间 */
  private LocalDateTime startTime;

  /** 结束时间 */
  private LocalDateTime endTime;
}
