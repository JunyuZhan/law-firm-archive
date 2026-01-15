package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日缓存实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_holiday_cache")
public class HolidayCache implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日期 */
    private LocalDate date;

    /** 年份 */
    private Integer year;

    /** 月份 */
    private Integer month;

    /** 日期类型：0=工作日, 1=周末, 2=法定节假日, 3=调休工作日 */
    private Integer dayType;

    /** 类型名称 */
    private String dayTypeName;

    /** 节假日名称（如"春节"） */
    private String holidayName;

    /** 是否休息日 */
    private Boolean isOff;

    /** 数据来源 */
    private String dataSource;

    /** 数据获取时间 */
    private LocalDateTime fetchedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // ===== 日期类型常量 =====
    public static final int TYPE_WORKDAY = 0;        // 工作日
    public static final int TYPE_WEEKEND = 1;        // 周末
    public static final int TYPE_HOLIDAY = 2;        // 法定节假日
    public static final int TYPE_WORKDAY_SHIFT = 3;  // 调休工作日
}
