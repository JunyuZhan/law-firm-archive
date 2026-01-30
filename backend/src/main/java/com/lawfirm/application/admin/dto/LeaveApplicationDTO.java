package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 请假申请DTO */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveApplicationDTO extends BaseDTO {
  /** 申请ID */
  private Long id;

  /** 申请编号 */
  private String applicationNo;

  /** 用户ID */
  private Long userId;

  /** 用户名称 */
  private String userName;

  /** 请假类型ID */
  private Long leaveTypeId;

  /** 请假类型名称 */
  private String leaveTypeName;

  /** 开始时间 */
  private LocalDateTime startTime;

  /** 结束时间 */
  private LocalDateTime endTime;

  /** 请假时长（天） */
  private BigDecimal duration;

  /** 请假原因 */
  private String reason;

  /** 附件URL */
  private String attachmentUrl;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 审批人ID */
  private Long approverId;

  /** 审批人名称 */
  private String approverName;

  /** 审批时间 */
  private LocalDateTime approvedAt;

  /** 审批意见 */
  private String approvalComment;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
