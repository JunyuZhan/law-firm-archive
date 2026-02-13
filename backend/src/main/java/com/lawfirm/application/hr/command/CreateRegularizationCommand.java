package com.lawfirm.application.hr.command;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

/** 创建转正申请命令 */
@Data
public class CreateRegularizationCommand {

  /** 员工ID */
  @NotNull(message = "员工ID不能为空")
  private Long employeeId;

  /** 试用期开始日期 */
  private LocalDate probationStartDate;

  /** 试用期结束日期 */
  private LocalDate probationEndDate;

  /** 预计转正日期 */
  private LocalDate expectedRegularDate;

  /** 自我评价 */
  private String selfEvaluation;
}
