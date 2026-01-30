package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 任务DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDTO extends BaseDTO {

  /** 主键ID. */
  private Long id;

  /** 任务编号. */
  private String taskNo;

  /** 案件ID. */
  private Long matterId;

  /** 案件名称. */
  private String matterName;

  /** 父任务ID. */
  private Long parentId;

  /** 任务标题. */
  private String title;

  /** 任务描述. */
  private String description;

  /** 优先级. */
  private String priority;

  /** 优先级名称. */
  private String priorityName;

  /** 负责人ID. */
  private Long assigneeId;

  /** 负责人姓名. */
  private String assigneeName;

  /** 开始日期. */
  private LocalDate startDate;

  /** 截止日期. */
  private LocalDate dueDate;

  /** 完成时间. */
  private LocalDateTime completedAt;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 进度. */
  private Integer progress;

  /** 提醒时间. */
  private LocalDateTime reminderDate;

  /** 是否已发送提醒. */
  private Boolean reminderSent;

  /** 是否逾期. */
  private Boolean overdue;

  /** 子任务数. */
  private Integer subTaskCount;

  /** 创建人ID. */
  private Long createdBy;

  /** 创建人姓名. */
  private String createdByName;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;

  /** 验收状态：PENDING_REVIEW-待验收, APPROVED-已通过, REJECTED-已退回. */
  private String reviewStatus;

  /** 验收意见. */
  private String reviewComment;

  /** 验收时间. */
  private LocalDateTime reviewedAt;

  /** 验收人ID. */
  private Long reviewedBy;
}
