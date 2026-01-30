package com.lawfirm.application.admin.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/** 加班申请命令（M8-004） */
@Data
public class ApplyOvertimeCommand {
  /** 加班日期 */
  @NotNull(message = "加班日期不能为空")
  private LocalDate overtimeDate;

  /** 开始时间 */
  @NotNull(message = "开始时间不能为空")
  private LocalTime startTime;

  /** 结束时间 */
  @NotNull(message = "结束时间不能为空")
  private LocalTime endTime;

  /** 加班原因 */
  private String reason;

  /** 工作内容 */
  private String workContent;
}
