package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateScheduleCommand;
import com.lawfirm.application.matter.dto.ScheduleDTO;
import com.lawfirm.application.matter.service.ScheduleAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程管理接口
 */
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleAppService scheduleAppService;

    /**
     * 查询日程
     */
    @GetMapping
    @RequirePermission("schedule:list")
    public Result<List<ScheduleDTO>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long matterId,
            @RequestParam(required = false) String scheduleType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "50") int pageSize) {
        return Result.success(scheduleAppService.listSchedules(userId, matterId, scheduleType, startTime, endTime, pageNum, pageSize));
    }

    /**
     * 获取日程详情
     */
    @GetMapping("/{id}")
    @RequirePermission("schedule:view")
    public Result<ScheduleDTO> getById(@PathVariable Long id) {
        return Result.success(scheduleAppService.getScheduleById(id));
    }

    /**
     * 创建日程
     */
    @PostMapping
    @RequirePermission("schedule:manage")
    @OperationLog(module = "日程管理", action = "创建日程")
    public Result<ScheduleDTO> create(@Valid @RequestBody CreateScheduleCommand command) {
        return Result.success(scheduleAppService.createSchedule(command));
    }

    /**
     * 更新日程
     */
    @PutMapping("/{id}")
    @RequirePermission("schedule:manage")
    @OperationLog(module = "日程管理", action = "更新日程")
    public Result<ScheduleDTO> update(@PathVariable Long id,
                                      @RequestParam(required = false) String title,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String location,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                                      @RequestParam(required = false) Integer reminderMinutes) {
        return Result.success(scheduleAppService.updateSchedule(id, title, description, location, startTime, endTime, reminderMinutes));
    }

    /**
     * 删除日程
     */
    @DeleteMapping("/{id}")
    @RequirePermission("schedule:manage")
    @OperationLog(module = "日程管理", action = "删除日程")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleAppService.deleteSchedule(id);
        return Result.success();
    }

    /**
     * 取消日程
     */
    @PostMapping("/{id}/cancel")
    @RequirePermission("schedule:manage")
    @OperationLog(module = "日程管理", action = "取消日程")
    public Result<Void> cancel(@PathVariable Long id) {
        scheduleAppService.cancelSchedule(id);
        return Result.success();
    }

    /**
     * 获取用户某天的日程
     */
    @GetMapping("/user/{userId}/date/{date}")
    @RequirePermission("schedule:list")
    public Result<List<ScheduleDTO>> getByUserAndDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(scheduleAppService.getSchedulesByDate(userId, date));
    }

    /**
     * 获取我今天的日程
     */
    @GetMapping("/my/today")
    @RequirePermission("schedule:view")
    public Result<List<ScheduleDTO>> getMyToday() {
        return Result.success(scheduleAppService.getMyTodaySchedules());
    }

    /**
     * 获取我近期的日程（用于工作台展示）
     */
    @GetMapping("/my/upcoming")
    @RequirePermission("schedule:view")
    public Result<List<ScheduleDTO>> getMyUpcoming(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(scheduleAppService.getMyUpcomingSchedules(days, limit));
    }
}
