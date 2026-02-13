package com.lawfirm.application.workbench.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 审批记录 DTO. */
@Data
public class ApprovalDTO {
  /** 主键ID. */
  private Long id;

  /** 审批编号. */
  private String approvalNo;

  /** 业务类型. */
  private String businessType;

  /** 业务类型名称. */
  private String businessTypeName;

  /** 业务ID. */
  private Long businessId;

  /** 业务编号. */
  private String businessNo;

  /** 业务标题. */
  private String businessTitle;

  /** 申请人ID. */
  private Long applicantId;

  /** 申请人姓名. */
  private String applicantName;

  /** 审批人ID. */
  private Long approverId;

  /** 审批人姓名. */
  private String approverName;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 审批意见. */
  private String comment;

  /** 审批时间. */
  private LocalDateTime approvedAt;

  /** 优先级. */
  private String priority;

  /** 优先级名称. */
  private String priorityName;

  /** 紧急程度. */
  private String urgency;

  /** 紧急程度名称. */
  private String urgencyName;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;

  /** 业务数据快照（JSON格式，保存审批时的业务数据）. */
  private String businessSnapshot;
}
