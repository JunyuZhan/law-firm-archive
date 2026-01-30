package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 更新期限提醒命令. */
@Data
public class UpdateDeadlineCommand {

  /** 期限ID. */
  @NotNull(message = "期限ID不能为空")
  private Long id;

  /** 期限名称. */
  private String deadlineName;

  /** 基准日期. */
  private LocalDate baseDate;

  /** 期限日期. */
  private LocalDate deadlineDate;

  /** 提醒天数. */
  private Integer reminderDays;

  /** 状态. */
  private String status;

  /** 描述. */
  private String description;
}
