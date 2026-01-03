package com.lawfirm.domain.admin.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 考勤记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("attendance")
public class Attendance extends BaseEntity {

    /** 员工ID */
    private Long userId;

    /** 考勤日期 */
    private LocalDate attendanceDate;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    /** 签退时间 */
    private LocalDateTime checkOutTime;

    /** 签到地点 */
    private String checkInLocation;

    /** 签退地点 */
    private String checkOutLocation;

    /** 签到设备 */
    private String checkInDevice;

    /** 签退设备 */
    private String checkOutDevice;

    /** 状态: NORMAL正常/LATE迟到/EARLY早退/ABSENT缺勤/LEAVE请假 */
    private String status;

    /** 工作时长(小时) */
    private BigDecimal workHours;

    /** 加班时长(小时) */
    private BigDecimal overtimeHours;

    /** 备注 */
    private String remark;

    // 状态常量
    public static final String STATUS_NORMAL = "NORMAL";
    public static final String STATUS_LATE = "LATE";
    public static final String STATUS_EARLY = "EARLY";
    public static final String STATUS_ABSENT = "ABSENT";
    public static final String STATUS_LEAVE = "LEAVE";
}
