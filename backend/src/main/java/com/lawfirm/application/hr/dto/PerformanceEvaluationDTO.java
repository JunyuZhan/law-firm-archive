package com.lawfirm.application.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 绩效评价DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceEvaluationDTO {

  /** 评价ID */
  private Long id;

  /** 任务ID */
  private Long taskId;

  /** 任务名称 */
  private String taskName;

  /** 员工ID */
  private Long employeeId;

  /** 员工姓名 */
  private String employeeName;

  /** 评价人ID */
  private Long evaluatorId;

  /** 评价人名称 */
  private String evaluatorName;

  /** 评价类型 */
  private String evaluationType;

  /** 评价类型名称 */
  private String evaluationTypeName;

  /** 总分 */
  private BigDecimal totalScore;

  /** 等级 */
  private String grade;

  /** 等级名称 */
  private String gradeName;

  /** 评价意见 */
  private String comment;

  /** 优点 */
  private String strengths;

  /** 改进建议 */
  private String improvements;

  /** 评价时间 */
  private LocalDateTime evaluatedAt;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 评分明细 */
  private List<PerformanceScoreDTO> scores;
}
