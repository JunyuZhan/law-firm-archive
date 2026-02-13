package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 加班申请DTO（M8-004） */
@Data
@EqualsAndHashCode(callSuper = true)
public class OvertimeApplicationDTO extends BaseDTO {
  /** 申请ID */
  private Long id;

  /** 申请编号 */
  private String applicationNo;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 加班日期 */
  private LocalDate overtimeDate;

  /** 开始时间 */
  private LocalTime startTime;

  /** 结束时间 */
  private LocalTime endTime;

  /** 加班时长（小时） */
  private BigDecimal overtimeHours;

  /** 加班原因 */
  private String reason;

  /** 工作内容 */
  private String workContent;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审批人ID */
  private Long approverId;

  /** 审批人名称 */
  private String approverName;

  /** 审批时间 */
  private java.time.LocalDateTime approvedAt;

  /** 审批意见 */
  private String approvalComment;
}
