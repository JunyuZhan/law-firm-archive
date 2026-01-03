package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会议预约DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingBookingDTO extends BaseDTO {
    private Long id;
    private String bookingNo;
    private Long roomId;
    private String roomName;
    private String title;
    private Long organizerId;
    private String organizerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<Long> attendeeIds;
    private String attendees;
    private String description;
    private String status;
    private String statusName;
    private LocalDateTime createdAt;
}
