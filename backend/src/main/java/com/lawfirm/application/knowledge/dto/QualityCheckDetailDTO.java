package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 质量检查明细DTO（M10-031）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckDetailDTO extends BaseDTO {
    private Long checkId;
    private Long standardId;
    private String checkResult;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String findings;
    private String suggestions;
}

