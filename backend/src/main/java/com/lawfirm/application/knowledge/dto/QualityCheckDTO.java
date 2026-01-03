package com.lawfirm.application.knowledge.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 质量检查DTO（M10-031）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QualityCheckDTO extends BaseDTO {
    private String checkNo;
    private Long matterId;
    private String matterName;
    private Long checkerId;
    private String checkerName;
    private LocalDate checkDate;
    private String checkType;
    private String checkTypeName;
    private String status;
    private String statusName;
    private BigDecimal totalScore;
    private Boolean qualified;
    private String checkSummary;
    private List<QualityCheckDetailDTO> details;
}

