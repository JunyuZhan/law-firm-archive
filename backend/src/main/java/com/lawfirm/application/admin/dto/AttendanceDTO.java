package com.lawfirm.application.admin.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttendanceDTO extends BaseDTO {
    private Long id;
    private Long userId;
    private String userName;
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String checkInLocation;
    private String checkOutLocation;
    private String status;
    private String statusName;
    private BigDecimal workHours;
    private BigDecimal overtimeHours;
    private String remark;
    private LocalDateTime createdAt;
}
