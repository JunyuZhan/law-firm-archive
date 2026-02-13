package com.lawfirm.application.hr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考核任务DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTaskDTO {

  /** 任务ID */
  private Long id;

  /** 任务名称 */
  private String name;

  /** 周期类型 */
  private String periodType;

  /** 周期类型名称 */
  private String periodTypeName;

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

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 描述 */
  private String description;

  /** 备注 */
  private String remarks;

  /** 创建时间 */
  private LocalDateTime createdAt;

  /** 员工总数 */
  private Integer totalEmployees;

  /** 已完成数量 */
  private Integer completedCount;
}
