package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CheckInCommand;
import com.lawfirm.application.admin.dto.AttendanceDTO;
import com.lawfirm.application.admin.dto.AttendanceQueryDTO;
import com.lawfirm.application.admin.dto.MonthlyAttendanceStatisticsDTO;
import com.lawfirm.application.admin.service.AttendanceAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 考勤管理接口 */
@Tag(name = "考勤管理", description = "考勤管理相关接口")
@RestController
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AttendanceController {

  /** 考勤应用服务 */
  private final AttendanceAppService attendanceAppService;

  /**
   * 分页查询考勤记录
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询考勤记录")
  @GetMapping
  @RequirePermission("admin:attendance:list")
  public Result<PageResult<AttendanceDTO>> list(final AttendanceQueryDTO query) {
    return Result.success(attendanceAppService.listAttendance(query));
  }

  /**
   * 签到
   *
   * @param command 签到命令
   * @return 考勤记录
   */
  @Operation(summary = "签到")
  @PostMapping("/check-in")
  @OperationLog(module = "考勤管理", action = "员工签到")
  public Result<AttendanceDTO> checkIn(@RequestBody final CheckInCommand command) {
    return Result.success(attendanceAppService.checkIn(command));
  }

  /**
   * 签退
   *
   * @param command 签退命令
   * @return 考勤记录
   */
  @Operation(summary = "签退")
  @PostMapping("/check-out")
  @OperationLog(module = "考勤管理", action = "员工签退")
  public Result<AttendanceDTO> checkOut(@RequestBody final CheckInCommand command) {
    return Result.success(attendanceAppService.checkOut(command));
  }

  /**
   * 获取今日考勤
   *
   * @return 考勤记录
   */
  @Operation(summary = "获取今日考勤")
  @GetMapping("/today")
  public Result<AttendanceDTO> getTodayAttendance() {
    return Result.success(attendanceAppService.getTodayAttendance());
  }

  /**
   * 获取月度考勤统计
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 统计结果
   */
  @Operation(summary = "获取月度考勤统计")
  @GetMapping("/statistics/monthly")
  public Result<MonthlyAttendanceStatisticsDTO> getMonthlyStatistics(
      @RequestParam(required = false) final Long userId,
      @RequestParam final Integer year,
      @RequestParam final Integer month) {
    return Result.success(attendanceAppService.getMonthlyStatistics(userId, year, month));
  }
}
