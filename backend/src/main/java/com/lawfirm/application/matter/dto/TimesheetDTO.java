package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 工时记录DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TimesheetDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 工时记录编号. */
  private String timesheetNo;

  /** 案件ID. */
  private Long matterId;

  /** 案件名称. */
  private String matterName;

  /** 案件编号. */
  private String matterNo;

  /** 用户ID. */
  private Long userId;

  /** 用户姓名. */
  private String userName;

  /** 工作日期. */
  private LocalDate workDate;

  /** 工时数. */
  private BigDecimal hours;

  /** 工作类型. */
  private String workType;

  /** 工作类型名称. */
  private String workTypeName;

  /** 工作内容. */
  private String workContent;

  /** 是否可计费. */
  private Boolean billable;

  /** 小时费率. */
  private BigDecimal hourlyRate;

  /** 金额. */
  private BigDecimal amount;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 提交时间. */
  private LocalDateTime submittedAt;

  /** 审批人ID. */
  private Long approvedBy;

  /** 审批人姓名. */
  private String approvedByName;

  /** 审批时间. */
  private LocalDateTime approvedAt;

  /** 审批意见. */
  private String approvalComment;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
