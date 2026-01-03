package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 工时查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TimesheetQueryDTO extends PageQuery {

    /**
     * 案件ID
     */
    private Long matterId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 工作类型
     */
    private String workType;

    /**
     * 状态
     */
    private String status;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 是否计费
     */
    private Boolean billable;
}
