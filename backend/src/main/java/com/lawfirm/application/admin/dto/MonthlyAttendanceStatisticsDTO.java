package com.lawfirm.application.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 月度考勤统计DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAttendanceStatisticsDTO {
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 年份
     */
    private Integer year;
    
    /**
     * 月份
     */
    private Integer month;
    
    /**
     * 正常出勤天数
     */
    private Integer normalDays;
    
    /**
     * 迟到天数
     */
    private Integer lateDays;
    
    /**
     * 早退天数
     */
    private Integer earlyDays;
    
    /**
     * 缺勤天数
     */
    private Integer absentDays;
    
    /**
     * 请假天数
     */
    private Integer leaveDays;
    
    /**
     * 总工作时长（小时）
     */
    private BigDecimal totalWorkHours;
    
    /**
     * 总加班时长（小时）
     */
    private BigDecimal totalOvertimeHours;
}

