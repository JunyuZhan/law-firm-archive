package com.lawfirm.application.workbench.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报表查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportQueryDTO extends PageQuery {

    /**
     * 报表类型
     */
    private String reportType;

    /**
     * 状态
     */
    private String status;

    /**
     * 生成人ID（查询我生成的报表）
     */
    private Long generatedBy;
}

