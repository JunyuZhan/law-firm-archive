package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.BookMeetingCommand;
import com.lawfirm.application.admin.command.CreateMeetingRoomCommand;
import com.lawfirm.application.admin.dto.MeetingBookingDTO;
import com.lawfirm.application.admin.dto.MeetingBookingQueryDTO;
import com.lawfirm.application.admin.dto.MeetingRoomDTO;
import com.lawfirm.application.admin.service.MeetingRoomAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 会议室管理接口
 */
@Tag(name = "会议室管理", description = "会议室管理相关接口")
@RestController
@RequestMapping("/admin/meeting-room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomAppService meetingRoomAppService;

    // ==================== 会议室管理 ====================

    @Operation(summary = "获取所有会议室")
    @GetMapping
    public Result<List<MeetingRoomDTO>> listRooms() {
        return Result.success(meetingRoomAppService.listRooms());
    }

    @Operation(summary = "获取可用会议室")
    @GetMapping("/available")
    public Result<List<MeetingRoomDTO>> listAvailableRooms() {
        return Result.success(meetingRoomAppService.listAvailableRooms());
    }

    @Operation(summary = "创建会议室")
    @PostMapping
    @RequirePermission("admin:meeting:manage")
    @OperationLog(module = "会议室管理", action = "创建会议室")
    public Result<MeetingRoomDTO> createRoom(@RequestBody CreateMeetingRoomCommand command) {
        return Result.success(meetingRoomAppService.createRoom(command));
    }

    @Operation(summary = "更新会议室")
    @PutMapping("/{id}")
    @RequirePermission("admin:meeting:manage")
    @OperationLog(module = "会议室管理", action = "更新会议室信息")
    public Result<MeetingRoomDTO> updateRoom(@PathVariable Long id, @RequestBody CreateMeetingRoomCommand command) {
        return Result.success(meetingRoomAppService.updateRoom(id, command));
    }

    @Operation(summary = "删除会议室")
    @DeleteMapping("/{id}")
    @RequirePermission("admin:meeting:manage")
    @OperationLog(module = "会议室管理", action = "删除会议室")
    public Result<Void> deleteRoom(@PathVariable Long id) {
        meetingRoomAppService.deleteRoom(id);
        return Result.success();
    }

    @Operation(summary = "更新会议室状态")
    @PutMapping("/{id}/status")
    @RequirePermission("admin:meeting:manage")
    @OperationLog(module = "会议室管理", action = "更新会议室状态")
    public Result<Void> updateRoomStatus(@PathVariable Long id, @RequestParam String status) {
        meetingRoomAppService.updateRoomStatus(id, status);
        return Result.success();
    }

    // ==================== 会议预约 ====================

    @Operation(summary = "分页查询会议预约")
    @GetMapping("/bookings")
    public Result<PageResult<MeetingBookingDTO>> listBookings(MeetingBookingQueryDTO query) {
        return Result.success(meetingRoomAppService.listBookings(query));
    }

    @Operation(summary = "预约会议室")
    @PostMapping("/bookings")
    @OperationLog(module = "会议室管理", action = "预约会议室")
    public Result<MeetingBookingDTO> bookMeeting(@RequestBody BookMeetingCommand command) {
        return Result.success(meetingRoomAppService.bookMeeting(command));
    }

    @Operation(summary = "取消会议预约")
    @PostMapping("/bookings/{id}/cancel")
    @OperationLog(module = "会议室管理", action = "取消会议预约")
    public Result<Void> cancelBooking(@PathVariable Long id) {
        meetingRoomAppService.cancelBooking(id);
        return Result.success();
    }

    @Operation(summary = "获取会议室某日预约情况")
    @GetMapping("/{roomId}/bookings/day")
    public Result<List<MeetingBookingDTO>> getRoomDayBookings(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(meetingRoomAppService.getRoomDayBookings(roomId, date));
    }

    @Operation(summary = "获取我的会议预约")
    @GetMapping("/bookings/my")
    public Result<List<MeetingBookingDTO>> getMyBookings() {
        return Result.success(meetingRoomAppService.getMyBookings());
    }

    @Operation(summary = "获取会议室日程视图（M8-024）")
    @GetMapping("/{roomId}/schedule")
    public Result<List<MeetingBookingDTO>> getRoomSchedule(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(meetingRoomAppService.getRoomSchedule(roomId, startDate, endDate));
    }

    @Operation(summary = "获取所有会议室日程视图（M8-024）")
    @GetMapping("/schedule/all")
    public Result<Map<Long, List<MeetingBookingDTO>>> getAllRoomsSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(meetingRoomAppService.getAllRoomsSchedule(startDate, endDate));
    }
}
