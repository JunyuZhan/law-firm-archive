package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 请假申请查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LeaveApplicationQueryDTO extends PageQuery {
    private Long userId;
    private Long leaveTypeId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
