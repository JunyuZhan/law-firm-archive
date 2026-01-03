package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 日程DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleDTO extends BaseDTO {

    private Long id;
    
    private Long matterId;
    private String matterName;
    
    private Long userId;
    private String userName;
    
    private String title;
    private String description;
    private String location;
    
    private String scheduleType;
    private String scheduleTypeName;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allDay;
    
    private Integer reminderMinutes;
    private Boolean reminderSent;
    
    private String recurrenceRule;
    
    private String status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
