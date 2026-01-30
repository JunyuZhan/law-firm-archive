package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 考勤记录DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceDTO extends BaseDTO {
  /** 记录ID */
  private Long id;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 考勤日期 */
  private LocalDate attendanceDate;

  /** 签到时间 */
  private LocalDateTime checkInTime;

  /** 签退时间 */
  private LocalDateTime checkOutTime;

  /** 签到地点 */
  private String checkInLocation;

  /** 签退地点 */
  private String checkOutLocation;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 工作时长（小时） */
  private BigDecimal workHours;

  /** 加班时长（小时） */
  private BigDecimal overtimeHours;

  /** 备注 */
  private String remark;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
