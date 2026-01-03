package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 会议记录DTO（M8-023）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingRecordDTO extends BaseDTO {
    private Long id;
    private String recordNo;
    private Long bookingId;
    private Long roomId;
    private String roomName;
    private String title;
    private LocalDate meetingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long organizerId;
    private String organizerName;
    private String attendees;
    private String content;
    private String decisions;
    private String actionItems;
    private String attachmentUrl;
}

