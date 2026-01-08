package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 会议预约查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingBookingQueryDTO extends PageQuery {
    private Long roomId;
    private Long organizerId;
    private String status;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;
}
