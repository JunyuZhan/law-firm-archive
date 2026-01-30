package com.lawfirm.domain.matter.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 任务实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("task")
public class Task extends BaseEntity {

  /** 任务编号 */
  private String taskNo;

  /** 案件ID */
  private Long matterId;

  /** 父任务ID */
  private Long parentId;

  /** 任务标题 */
  private String title;

  /** 任务描述 */
  private String description;

  /** 优先级 */
  @lombok.Builder.Default private String priority = "MEDIUM";

  /** 执行人ID */
  private Long assigneeId;

  /** 执行人姓名 */
  private String assigneeName;

  /** 开始日期 */
  private LocalDate startDate;

  /** 截止日期 */
  private LocalDate dueDate;

  /** 完成时间 */
  private LocalDateTime completedAt;

  /** 状态 */
  @lombok.Builder.Default private String status = "TODO";

  /** 进度（0-100） */
  @lombok.Builder.Default private Integer progress = 0;

  /** 提醒时间 */
  private LocalDateTime reminderDate;

  /** 提醒是否已发送 */
  @lombok.Builder.Default private Boolean reminderSent = false;

  /** 验收状态：PENDING_REVIEW-待验收, APPROVED-已通过, REJECTED-已退回 */
  private String reviewStatus;

  /** 验收意见（退回时填写） */
  private String reviewComment;

  /** 验收时间 */
  private LocalDateTime reviewedAt;

  /** 验收人ID */
  private Long reviewedBy;
}
