package com.lawfirm.application.matter.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 创建期限提醒命令. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeadlineCommand {

  /** 项目ID. */
  @NotNull(message = "项目ID不能为空")
  private Long matterId;

  /** 期限类型. */
  @NotBlank(message = "期限类型不能为空")
  private String deadlineType;

  /** 期限名称. */
  @NotBlank(message = "期限名称不能为空")
  private String deadlineName;

  /** 基准日期（可选，默认使用当前日期）. */
  private LocalDate baseDate;

  /** 期限日期. */
  @NotNull(message = "期限日期不能为空")
  private LocalDate deadlineDate;

  /** 提醒天数（默认7天）. */
  private Integer reminderDays;

  /** 描述. */
  private String description;
}
