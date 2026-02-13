package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质量检查明细DTO（M10-031） */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckDetailDTO extends BaseDTO {
  /** 检查ID */
  private Long checkId;

  /** 标准ID */
  private Long standardId;

  /** 检查结果 */
  private String checkResult;

  /** 得分 */
  private BigDecimal score;

  /** 最高分 */
  private BigDecimal maxScore;

  /** 检查发现 */
  private String findings;

  /** 改进建议 */
  private String suggestions;
}
