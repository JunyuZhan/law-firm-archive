package com.lawfirm.application.hr.command;

import java.time.LocalDate;
import lombok.Data;

/** 创建考核任务命令 */
@Data
public class CreatePerformanceTaskCommand {

  /** 任务名称 */
  private String name;

  /** 周期类型 */
  private String periodType;

  /** 年份 */
  private Integer year;

  /** 周期 */
  private Integer period;

  /** 开始日期 */
  private LocalDate startDate;

  /** 结束日期 */
  private LocalDate endDate;

  /** 自评截止日期 */
  private LocalDate selfEvalDeadline;

  /** 互评截止日期 */
  private LocalDate peerEvalDeadline;

  /** 主管评价截止日期 */
  private LocalDate supervisorEvalDeadline;

  /** 描述 */
  private String description;

  /** 备注 */
  private String remarks;
}
