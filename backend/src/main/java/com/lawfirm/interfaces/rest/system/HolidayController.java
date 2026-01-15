package com.lawfirm.interfaces.rest.system;

import com.lawfirm.common.result.Result;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.infrastructure.external.holiday.HolidayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节假日服务控制器
 * 
 * 提供工作日计算、诉讼期限计算等API
 */
@Tag(name = "节假日服务", description = "工作日计算、诉讼期限计算")
@RestController
@RequestMapping("/system/holiday")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "判断是否工作日", description = "判断指定日期是否为工作日（排除周末和法定节假日，但包含调休工作日）")
    @GetMapping("/is-workday")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> isWorkday(
            @Parameter(description = "日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        boolean workday = holidayService.isWorkday(date);
        String typeName = holidayService.getDayTypeName(date);
        
        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("isWorkday", workday);
        result.put("typeName", typeName);
        
        return Result.success(result);
    }

    @Operation(summary = "判断是否法定节假日", description = "判断指定日期是否为法定节假日（不含周末）")
    @GetMapping("/is-holiday")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> isHoliday(
            @Parameter(description = "日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        
        boolean holiday = holidayService.isHoliday(date);
        String typeName = holidayService.getDayTypeName(date);
        
        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("isHoliday", holiday);
        result.put("typeName", typeName);
        
        return Result.success(result);
    }

    @Operation(summary = "计算N个工作日后的日期", description = "从指定日期开始，计算N个工作日后的日期")
    @GetMapping("/add-workdays")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> addWorkdays(
            @Parameter(description = "起始日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "工作日数量（正数向后，负数向前）") 
            @RequestParam int workdays) {
        
        LocalDate resultDate = holidayService.addWorkdays(startDate, workdays);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("workdays", workdays);
        result.put("resultDate", resultDate);
        result.put("isWorkday", holidayService.isWorkday(resultDate));
        
        return Result.success(result);
    }

    @Operation(summary = "计算诉讼期限截止日期", 
               description = "根据起算日和期限天数计算截止日期，自动处理节假日顺延。" +
                            "根据《民事诉讼法》规定，期间从次日起算，届满日是节假日的顺延到下一工作日。")
    @GetMapping("/deadline")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> calculateDeadline(
            @Parameter(description = "起算日期（如判决书送达日），格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "期限天数") 
            @RequestParam int days,
            @Parameter(description = "是否按工作日计算（默认false，即按自然日计算但节假日顺延）") 
            @RequestParam(defaultValue = "false") boolean workdaysOnly) {
        
        LocalDate deadline = holidayService.calculateDeadline(startDate, days, workdaysOnly);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("days", days);
        result.put("workdaysOnly", workdaysOnly);
        result.put("deadline", deadline);
        result.put("isWorkday", holidayService.isWorkday(deadline));
        result.put("deadlineTypeName", holidayService.getDayTypeName(deadline));
        
        // 计算说明
        String explanation;
        if (workdaysOnly) {
            explanation = String.format("从%s次日起算%d个工作日", startDate, days);
        } else {
            explanation = String.format("从%s次日起算%d天（节假日顺延）", startDate, days);
        }
        result.put("explanation", explanation);
        
        return Result.success(result);
    }

    @Operation(summary = "计算两个日期间的工作日数量", description = "统计指定日期范围内的工作日数量（包含起止日期）")
    @GetMapping("/count-workdays")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> countWorkdays(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        int count = holidayService.countWorkdays(startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("workdayCount", count);
        
        return Result.success(result);
    }

    @Operation(summary = "获取范围内的休息日", description = "获取指定日期范围内的所有休息日列表")
    @GetMapping("/off-days")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> getOffDays(
            @Parameter(description = "开始日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        
        List<LocalDate> offDays = holidayService.getOffDaysInRange(startDate, endDate);
        
        Map<String, Object> result = new HashMap<>();
        result.put("startDate", startDate);
        result.put("endDate", endDate);
        result.put("offDays", offDays);
        result.put("count", offDays.size());
        
        return Result.success(result);
    }

    @Operation(summary = "计算提醒日期", description = "计算在目标日期前N个工作日的日期（用于设置提前提醒）")
    @GetMapping("/reminder-date")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> calculateReminderDate(
            @Parameter(description = "目标日期（如开庭日期），格式：yyyy-MM-dd") 
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate targetDate,
            @Parameter(description = "提前几个工作日提醒") 
            @RequestParam int workdaysBefore) {
        
        LocalDate reminderDate = holidayService.calculateReminderDate(targetDate, workdaysBefore);
        
        Map<String, Object> result = new HashMap<>();
        result.put("targetDate", targetDate);
        result.put("workdaysBefore", workdaysBefore);
        result.put("reminderDate", reminderDate);
        
        return Result.success(result);
    }

    @Operation(summary = "同步节假日数据", description = "管理员手动触发同步指定年份的节假日数据到本地缓存")
    @PostMapping("/sync/{year}")
    @RequirePermission("system:config:manage")
    public Result<Map<String, Object>> syncHolidays(
            @Parameter(description = "年份") 
            @PathVariable int year) {
        
        int count = holidayService.syncYearHolidays(year);
        
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("syncedCount", count);
        result.put("message", String.format("成功同步%d年节假日数据，共%d条", year, count));
        
        return Result.success(result);
    }

    @Operation(summary = "检查数据同步状态", description = "检查指定年份的节假日数据是否已同步")
    @GetMapping("/sync-status/{year}")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, Object>> checkSyncStatus(
            @Parameter(description = "年份") 
            @PathVariable int year) {
        
        boolean synced = holidayService.isYearSynced(year);
        
        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("synced", synced);
        
        return Result.success(result);
    }
}
