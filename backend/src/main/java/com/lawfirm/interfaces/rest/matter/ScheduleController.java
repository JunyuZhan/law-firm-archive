package com.lawfirm.interfaces.rest.matter;

import com.lawfirm.application.matter.command.CreateScheduleCommand;
import com.lawfirm.application.matter.dto.ScheduleDTO;
import com.lawfirm.application.matter.service.ScheduleAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 日程管理接口 */
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

  /** 日程应用服务 */
  private final ScheduleAppService scheduleAppService;

  /**
   * 查询日程
   *
   * @param userId 用户ID
   * @param matterId 案件ID
   * @param scheduleType 日程类型
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @return 日程列表
   */
  @GetMapping
  @RequirePermission("schedule:list")
  public Result<List<ScheduleDTO>> list(
      @RequestParam(required = false) final Long userId,
      @RequestParam(required = false) final Long matterId,
      @RequestParam(required = false) final String scheduleType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          final LocalDateTime startTime,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          final LocalDateTime endTime,
      @RequestParam(defaultValue = "1") final int pageNum,
      @RequestParam(defaultValue = "50") final int pageSize) {
    return Result.success(
        scheduleAppService.listSchedules(
            userId, matterId, scheduleType, startTime, endTime, pageNum, pageSize));
  }

  /**
   * 获取日程详情
   *
   * @param id 日程ID
   * @return 日程信息
   */
  @GetMapping("/{id}")
  @RequirePermission("schedule:view")
  public Result<ScheduleDTO> getById(@PathVariable final Long id) {
    return Result.success(scheduleAppService.getScheduleById(id));
  }

  /**
   * 创建日程
   *
   * @param command 创建日程命令
   * @return 日程信息
   */
  @PostMapping
  @RequirePermission("schedule:manage")
  @OperationLog(module = "日程管理", action = "创建日程")
  public Result<ScheduleDTO> create(@Valid @RequestBody final CreateScheduleCommand command) {
    return Result.success(scheduleAppService.createSchedule(command));
  }

  /**
   * 更新日程
   *
   * @param id 日程ID
   * @param title 标题
   * @param description 描述
   * @param location 地点
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param reminderMinutes 提醒分钟数
   * @return 日程信息
   */
  @PutMapping("/{id}")
  @RequirePermission("schedule:manage")
  @OperationLog(module = "日程管理", action = "更新日程")
  public Result<ScheduleDTO> update(
      @PathVariable final Long id,
      @RequestParam(required = false) final String title,
      @RequestParam(required = false) final String description,
      @RequestParam(required = false) final String location,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          final LocalDateTime startTime,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          final LocalDateTime endTime,
      @RequestParam(required = false) final Integer reminderMinutes) {
    return Result.success(
        scheduleAppService.updateSchedule(
            id, title, description, location, startTime, endTime, reminderMinutes));
  }

  /**
   * 删除日程
   *
   * @param id 日程ID
   * @return 空结果
   */
  @DeleteMapping("/{id}")
  @RequirePermission("schedule:manage")
  @OperationLog(module = "日程管理", action = "删除日程")
  public Result<Void> delete(@PathVariable final Long id) {
    scheduleAppService.deleteSchedule(id);
    return Result.success();
  }

  /**
   * 取消日程
   *
   * @param id 日程ID
   * @return 空结果
   */
  @PostMapping("/{id}/cancel")
  @RequirePermission("schedule:manage")
  @OperationLog(module = "日程管理", action = "取消日程")
  public Result<Void> cancel(@PathVariable final Long id) {
    scheduleAppService.cancelSchedule(id);
    return Result.success();
  }

  /**
   * 获取用户某天的日程
   *
   * @param userId 用户ID
   * @param date 日期
   * @return 日程列表
   */
  @GetMapping("/user/{userId}/date/{date}")
  @RequirePermission("schedule:list")
  public Result<List<ScheduleDTO>> getByUserAndDate(
      @PathVariable final Long userId,
      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date) {
    return Result.success(scheduleAppService.getSchedulesByDate(userId, date));
  }

  /**
   * 获取我今天的日程
   *
   * @return 日程列表
   */
  @GetMapping("/my/today")
  @RequirePermission("schedule:view")
  public Result<List<ScheduleDTO>> getMyToday() {
    return Result.success(scheduleAppService.getMyTodaySchedules());
  }

  /**
   * 获取我近期的日程（用于工作台展示）
   *
   * @param days 天数
   * @param limit 限制数量
   * @return 日程列表
   */
  @GetMapping("/my/upcoming")
  @RequirePermission("schedule:view")
  public Result<List<ScheduleDTO>> getMyUpcoming(
      @RequestParam(defaultValue = "7") final int days,
      @RequestParam(defaultValue = "10") final int limit) {
    return Result.success(scheduleAppService.getMyUpcomingSchedules(days, limit));
  }
}
