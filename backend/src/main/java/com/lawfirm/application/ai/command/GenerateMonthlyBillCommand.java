package com.lawfirm.application.ai.command;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/** 生成月度账单命令 */
@Data
public class GenerateMonthlyBillCommand {

  /** 账单年份 */
  @Min(value = 2020, message = "年份不能小于2020")
  @Max(value = 2100, message = "年份不能大于2100")
  private Integer year;

  /** 账单月份（1-12） */
  @Min(value = 1, message = "月份必须在1-12之间")
  @Max(value = 12, message = "月份必须在1-12之间")
  private Integer month;

  /** 是否重新生成（覆盖已存在的账单） */
  private Boolean regenerate = false;
}
