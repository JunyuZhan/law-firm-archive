package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 会议预约查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingBookingQueryDTO extends PageQuery {
    private Long roomId;
    private Long organizerId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
