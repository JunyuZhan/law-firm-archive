package com.lawfirm.infrastructure.external.holiday;

import com.lawfirm.domain.system.entity.HolidayCache;
import com.lawfirm.infrastructure.external.holiday.dto.HolidayInfo;
import com.lawfirm.infrastructure.persistence.mapper.HolidayCacheMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日服务
 * 
 * 提供工作日计算、节假日判断等功能
 * 优先使用本地缓存，缓存未命中时调用API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayClient holidayClient;
    private final HolidayCacheMapper holidayCacheMapper;

    /**
     * 判断是否为工作日
     * 
     * @param date 日期
     * @return true=工作日, false=休息日
     */
    public boolean isWorkday(LocalDate date) {
        // 1. 查本地缓存
        HolidayCache cache = holidayCacheMapper.selectByDate(date);
        if (cache != null) {
            return !cache.getIsOff();
        }
        
        // 2. 调用API
        HolidayInfo info = holidayClient.getHolidayInfo(date);
        if (info != null) {
            saveToCache(info);
            return !info.isOff();
        }
        
        // 3. 兜底：周末为非工作日
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为节假日（仅法定节假日，不含周末）
     * 
     * @param date 日期
     * @return true=法定节假日
     */
    public boolean isHoliday(LocalDate date) {
        HolidayCache cache = holidayCacheMapper.selectByDate(date);
        if (cache != null) {
            return cache.getDayType() == HolidayCache.TYPE_HOLIDAY;
        }
        
        HolidayInfo info = holidayClient.getHolidayInfo(date);
        if (info != null) {
            saveToCache(info);
            return info.getDayType() == HolidayCache.TYPE_HOLIDAY;
        }
        
        return false;
    }

    /**
     * 获取日期类型名称
     * 
     * @param date 日期
     * @return 类型名称（如"工作日"、"周末"、"春节"）
     */
    public String getDayTypeName(LocalDate date) {
        HolidayCache cache = holidayCacheMapper.selectByDate(date);
        if (cache != null) {
            if (cache.getHolidayName() != null) {
                return cache.getHolidayName();
            }
            return cache.getDayTypeName();
        }
        
        HolidayInfo info = holidayClient.getHolidayInfo(date);
        if (info != null) {
            saveToCache(info);
            if (info.getHolidayName() != null) {
                return info.getHolidayName();
            }
            return info.getDayTypeName();
        }
        
        // 兜底
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return "周末";
        }
        return "工作日";
    }

    /**
     * 计算N个工作日后的日期
     * 
     * @param startDate 起始日期
     * @param workdays 工作日数量（正数向后计算，负数向前计算）
     * @return 目标日期
     */
    public LocalDate addWorkdays(LocalDate startDate, int workdays) {
        // 先尝试使用API计算（更准确）
        LocalDate apiResult = holidayClient.calculateWorkday(startDate, workdays);
        if (apiResult != null) {
            return apiResult;
        }
        
        // 兜底：使用本地缓存计算
        return calculateWorkdaysLocal(startDate, workdays);
    }

    /**
     * 计算两个日期之间的工作日数量
     * 
     * @param startDate 开始日期（包含）
     * @param endDate 结束日期（包含）
     * @return 工作日数量
     */
    public int countWorkdays(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            return -countWorkdays(endDate, startDate);
        }
        
        int count = 0;
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (isWorkday(current)) {
                count++;
            }
            current = current.plusDays(1);
        }
        
        return count;
    }

    /**
     * 计算诉讼期限截止日期
     * 
     * 根据《民事诉讼法》规定，期间以日计算的，从期间开始的次日起算
     * 期间届满的最后一日是节假日的，以节假日后的第一日为期间届满的日期
     * 
     * @param startDate 起算日期（如判决书送达日）
     * @param days 期限天数
     * @param workdaysOnly 是否按工作日计算（true=按工作日，false=按自然日但节假日顺延）
     * @return 截止日期
     */
    public LocalDate calculateDeadline(LocalDate startDate, int days, boolean workdaysOnly) {
        // 从次日起算
        LocalDate calcStart = startDate.plusDays(1);
        
        if (workdaysOnly) {
            // 按工作日计算
            return addWorkdays(calcStart, days - 1);
        } else {
            // 按自然日计算，但如果到期日是节假日则顺延到下一个工作日
            LocalDate deadline = calcStart.plusDays(days - 1);
            while (!isWorkday(deadline)) {
                deadline = deadline.plusDays(1);
            }
            return deadline;
        }
    }

    /**
     * 计算提前N个工作日提醒的日期
     * 
     * @param targetDate 目标日期（如开庭日期）
     * @param workdaysBefore 提前几个工作日
     * @return 提醒日期
     */
    public LocalDate calculateReminderDate(LocalDate targetDate, int workdaysBefore) {
        return addWorkdays(targetDate, -workdaysBefore);
    }

    /**
     * 获取指定范围内的所有休息日
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 休息日列表
     */
    public List<LocalDate> getOffDaysInRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> offDays = new ArrayList<>();
        LocalDate current = startDate;
        
        while (!current.isAfter(endDate)) {
            if (!isWorkday(current)) {
                offDays.add(current);
            }
            current = current.plusDays(1);
        }
        
        return offDays;
    }

    /**
     * 同步指定年份的节假日数据到本地缓存
     * 
     * @param year 年份
     * @return 同步的记录数
     */
    public int syncYearHolidays(int year) {
        List<HolidayInfo> holidays = holidayClient.getYearHolidays(year);
        
        if (holidays.isEmpty()) {
            log.warn("获取{}年节假日数据为空", year);
            return 0;
        }
        
        List<HolidayCache> cacheList = new ArrayList<>();
        for (HolidayInfo info : holidays) {
            cacheList.add(convertToCache(info));
        }
        
        // 批量插入或更新
        int count = holidayCacheMapper.batchInsertOrUpdate(cacheList);
        
        log.info("同步{}年节假日数据完成，共{}条", year, count);
        return count;
    }

    /**
     * 检查某年数据是否已同步
     * 
     * @param year 年份
     * @return true=已有数据
     */
    public boolean isYearSynced(int year) {
        return holidayCacheMapper.countByYear(year) > 0;
    }

    /**
     * 本地计算工作日（使用缓存数据）
     */
    private LocalDate calculateWorkdaysLocal(LocalDate startDate, int workdays) {
        LocalDate current = startDate;
        int direction = workdays > 0 ? 1 : -1;
        int remaining = Math.abs(workdays);
        
        while (remaining > 0) {
            current = current.plusDays(direction);
            if (isWorkday(current)) {
                remaining--;
            }
        }
        
        return current;
    }

    /**
     * 保存到本地缓存
     */
    private void saveToCache(HolidayInfo info) {
        try {
            HolidayCache cache = convertToCache(info);
            holidayCacheMapper.insertOrUpdate(cache);
        } catch (Exception e) {
            log.warn("保存节假日缓存失败: date={}", info.getDate(), e);
        }
    }

    /**
     * 转换为缓存实体
     */
    private HolidayCache convertToCache(HolidayInfo info) {
        return HolidayCache.builder()
                .date(info.getDate())
                .year(info.getDate().getYear())
                .month(info.getDate().getMonthValue())
                .dayType(info.getDayType())
                .dayTypeName(info.getDayTypeName())
                .holidayName(info.getHolidayName())
                .isOff(info.isOff())
                .dataSource("TIMOR")
                .fetchedAt(LocalDateTime.now())
                .build();
    }
}
