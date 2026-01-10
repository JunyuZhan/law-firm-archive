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
import org.springframework.web.bind.annotation.*;

/**
 * 考勤管理接口
 */
@Tag(name = "考勤管理", description = "考勤管理相关接口")
@RestController
@RequestMapping("/admin/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceAppService attendanceAppService;

    @Operation(summary = "分页查询考勤记录")
    @GetMapping
    @RequirePermission("admin:attendance:list")
    public Result<PageResult<AttendanceDTO>> list(AttendanceQueryDTO query) {
        return Result.success(attendanceAppService.listAttendance(query));
    }

    @Operation(summary = "签到")
    @PostMapping("/check-in")
    @OperationLog(module = "考勤管理", action = "员工签到")
    public Result<AttendanceDTO> checkIn(@RequestBody CheckInCommand command) {
        return Result.success(attendanceAppService.checkIn(command));
    }

    @Operation(summary = "签退")
    @PostMapping("/check-out")
    @OperationLog(module = "考勤管理", action = "员工签退")
    public Result<AttendanceDTO> checkOut(@RequestBody CheckInCommand command) {
        return Result.success(attendanceAppService.checkOut(command));
    }

    @Operation(summary = "获取今日考勤")
    @GetMapping("/today")
    public Result<AttendanceDTO> getTodayAttendance() {
        return Result.success(attendanceAppService.getTodayAttendance());
    }

    @Operation(summary = "获取月度考勤统计")
    @GetMapping("/statistics/monthly")
    public Result<MonthlyAttendanceStatisticsDTO> getMonthlyStatistics(
            @RequestParam(required = false) Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month) {
        return Result.success(attendanceAppService.getMonthlyStatistics(userId, year, month));
    }
}
