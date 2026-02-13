package com.lawfirm.application.hr.command;

import java.math.BigDecimal;
import lombok.Data;

/** 创建考核指标命令 */
@Data
public class CreateIndicatorCommand {

  /** 指标名称 */
  private String name;

  /** 指标编码 */
  private String code;

  /** 指标类别 */
  private String category;

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

  /** 排序 */
  private Integer sortOrder;

  /** 备注 */
  private String remarks;
}
