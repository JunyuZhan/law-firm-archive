package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 创建任务命令 */
@Data
public class CreateTaskCommand {

  /** 案件ID. */
  @NotNull(message = "案件ID不能为空")
  private Long matterId;

  /** 父任务ID. */
  private Long parentId;

  /** 任务标题. */
  @NotBlank(message = "任务标题不能为空")
  private String title;

  /** 任务描述. */
  private String description;

  /** 优先级. */
  private String priority;

  /** 执行人ID. */
  private Long assigneeId;

  /** 执行人姓名. */
  private String assigneeName;

  /** 开始日期. */
  private LocalDate startDate;

  /** 截止日期. */
  private LocalDate dueDate;

  /** 提醒时间. */
  private LocalDateTime reminderDate;
}
