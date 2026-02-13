package com.lawfirm.application.hr.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 绩效评分明细DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceScoreDTO {

  /** 评分ID */
  private Long id;

  /** 评价ID */
  private Long evaluationId;

  /** 指标ID */
  private Long indicatorId;

  /** 指标名称 */
  private String indicatorName;

  /** 指标类别 */
  private String indicatorCategory;

  /** 权重 */
  private BigDecimal weight;

  /** 最高分 */
  private Integer maxScore;

  /** 得分 */
  private BigDecimal score;

  /** 评价意见 */
  private String comment;
}
