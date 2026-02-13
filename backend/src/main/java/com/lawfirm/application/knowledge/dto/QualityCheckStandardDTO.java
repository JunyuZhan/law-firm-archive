package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质量检查标准DTO（M10-030） */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckStandardDTO extends BaseDTO {
  /** 标准编号 */
  private String standardNo;

  /** 标准名称 */
  private String standardName;

  /** 类别 */
  private String category;

  /** 类别名称 */
  private String categoryName;

  /** 描述 */
  private String description;

  /** 检查项 */
  private String checkItems;

  /** 适用案件类型 */
  private String applicableMatterTypes;

  /** 权重 */
  private BigDecimal weight;

  /** 是否启用 */
  private Boolean enabled;

  /** 排序 */
  private Integer sortOrder;
}
