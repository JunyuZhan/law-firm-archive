package com.lawfirm.application.hr.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 考核指标DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceIndicatorDTO {

  /** 指标ID */
  private Long id;

  /** 指标名称 */
  private String name;

  /** 指标编码 */
  private String code;

  /** 指标类别 */
  private String category;

  /** 指标类别名称 */
  private String categoryName;

  /** 描述 */
  private String description;

  /** 权重 */
  private BigDecimal weight;

  /** 最高分 */
  private Integer maxScore;

  /** 评分标准 */
  private String scoringCriteria;

  /** 适用角色 */
  private String applicableRole;

  /** 适用角色名称 */
  private String applicableRoleName;

  /** 排序 */
  private Integer sortOrder;

  /** 状态 */
  private String status;

  /** 备注 */
  private String remarks;
}
