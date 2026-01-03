package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 加班申请DTO（M8-004）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OvertimeApplicationDTO extends BaseDTO {
    private Long id;
    private String applicationNo;
    private Long userId;
    private String userName;
    private LocalDate overtimeDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal overtimeHours;
    private String reason;
    private String workContent;
    private String status;
    private String statusName;
    private Long approverId;
    private String approverName;
    private java.time.LocalDateTime approvedAt;
    private String approvalComment;
}

