package com.lawfirm.domain.hr.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 考核任务实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("hr_performance_task")
public class PerformanceTask extends BaseEntity {

  /** 考核任务名称 */
  private String name;

  /** 考核周期类型：MONTHLY-月度, QUARTERLY-季度, YEARLY-年度 */
  private String periodType;

  /** 考核年份 */
  private Integer year;

  /** 考核周期（月/季度） */
  private Integer period;

  /** 考核开始日期 */
  private LocalDate startDate;

  /** 考核结束日期 */
  private LocalDate endDate;

  /** 自评截止日期 */
  private LocalDate selfEvalDeadline;

  /** 互评截止日期 */
  private LocalDate peerEvalDeadline;

  /** 上级评价截止日期 */
  private LocalDate supervisorEvalDeadline;

  /** 状态：DRAFT-草稿, IN_PROGRESS-进行中, COMPLETED-已完成, CANCELLED-已取消 */
  private String status;

  /** 考核说明 */
  private String description;

  /** 备注 */
  private String remarks;
}
