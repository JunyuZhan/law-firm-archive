package com.lawfirm.application.matter.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 期限提醒 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeadlineDTO extends BaseDTO {

    private Long matterId;
    private String matterNo;
    private String matterName;
    private String deadlineType;
    private String deadlineTypeName;
    private String deadlineName;
    private LocalDate baseDate;
    private LocalDate deadlineDate;
    private Integer reminderDays;
    private Boolean reminderSent;
    private LocalDateTime reminderSentAt;
    private String status;
    private String statusName;
    private LocalDateTime completedAt;
    private Long completedBy;
    private String completedByName;
    private String description;

    /**
     * 剩余天数（负数表示已过期）
     */
    private Long daysRemaining;
}

