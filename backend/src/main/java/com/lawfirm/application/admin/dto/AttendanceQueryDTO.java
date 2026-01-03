package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 考勤查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceQueryDTO extends PageQuery {
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
