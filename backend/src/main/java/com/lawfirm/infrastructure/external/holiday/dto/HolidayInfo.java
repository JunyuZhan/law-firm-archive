package com.lawfirm.infrastructure.external.holiday.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 节假日信息DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayInfo {

    /** 日期 */
    private LocalDate date;

    /** 日期类型：0=工作日, 1=周末, 2=法定节假日, 3=调休工作日 */
    private int dayType;

    /** 类型名称 */
    private String dayTypeName;

    /** 节假日名称（如"春节"、"国庆节"） */
    private String holidayName;

    /** 是否休息日 */
    private boolean isOff;
}
