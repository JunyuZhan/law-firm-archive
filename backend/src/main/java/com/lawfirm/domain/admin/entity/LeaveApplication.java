package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 请假申请实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("leave_application")
public class LeaveApplication extends BaseEntity {

  /** 申请编号. */
  private String applicationNo;

  /** 申请人ID. */
  private Long userId;

  /** 请假类型ID. */
  private Long leaveTypeId;

  /** 开始时间. */
  private LocalDateTime startTime;

  /** 结束时间. */
  private LocalDateTime endTime;

  /** 请假时长(天). */
  private BigDecimal duration;

  /** 请假原因. */
  private String reason;

  /** 附件URL. */
  private String attachmentUrl;

  /** 状态: PENDING待审批/APPROVED已批准/REJECTED已拒绝/CANCELLED已取消. */
  private String status;

  /** 审批人ID. */
  private Long approverId;

  /** 审批时间. */
  private LocalDateTime approvedAt;

  /** 审批意见. */
  private String approvalComment;

  // 状态常量
  /** 状态：待审批. */
  public static final String STATUS_PENDING = "PENDING";

  /** 状态：已批准. */
  public static final String STATUS_APPROVED = "APPROVED";

  /** 状态：已拒绝. */
  public static final String STATUS_REJECTED = "REJECTED";

  /** 状态：已取消. */
  public static final String STATUS_CANCELLED = "CANCELLED";
}
