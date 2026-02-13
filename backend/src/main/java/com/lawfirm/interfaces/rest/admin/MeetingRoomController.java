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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

/** 会议室管理接口 */
@Tag(name = "会议室管理", description = "会议室管理相关接口")
@RestController
@RequestMapping("/admin/meeting-room")
@RequiredArgsConstructor
public class MeetingRoomController {

  /** 会议室应用服务 */
  private final MeetingRoomAppService meetingRoomAppService;

  // ==================== 会议室管理 ====================

  /**
   * 获取所有会议室
   *
   * @return 会议室列表
   */
  @Operation(summary = "获取所有会议室")
  @GetMapping
  public Result<List<MeetingRoomDTO>> listRooms() {
    return Result.success(meetingRoomAppService.listRooms());
  }

  /**
   * 获取可用会议室
   *
   * @return 会议室列表
   */
  @Operation(summary = "获取可用会议室")
  @GetMapping("/available")
  public Result<List<MeetingRoomDTO>> listAvailableRooms() {
    return Result.success(meetingRoomAppService.listAvailableRooms());
  }

  /**
   * 创建会议室
   *
   * @param command 创建命令
   * @return 会议室信息
   */
  @Operation(summary = "创建会议室")
  @PostMapping
  @RequirePermission("admin:meeting:manage")
  @OperationLog(module = "会议室管理", action = "创建会议室")
  public Result<MeetingRoomDTO> createRoom(@RequestBody final CreateMeetingRoomCommand command) {
    return Result.success(meetingRoomAppService.createRoom(command));
  }

  /**
   * 更新会议室
   *
   * @param id 会议室ID
   * @param command 更新命令
   * @return 会议室信息
   */
  @Operation(summary = "更新会议室")
  @PutMapping("/{id}")
  @RequirePermission("admin:meeting:manage")
  @OperationLog(module = "会议室管理", action = "更新会议室信息")
  public Result<MeetingRoomDTO> updateRoom(
      @PathVariable final Long id, @RequestBody final CreateMeetingRoomCommand command) {
    return Result.success(meetingRoomAppService.updateRoom(id, command));
  }

  /**
   * 删除会议室
   *
   * @param id 会议室ID
   * @return 无返回
   */
  @Operation(summary = "删除会议室")
  @DeleteMapping("/{id}")
  @RequirePermission("admin:meeting:manage")
  @OperationLog(module = "会议室管理", action = "删除会议室")
  public Result<Void> deleteRoom(@PathVariable final Long id) {
    meetingRoomAppService.deleteRoom(id);
    return Result.success();
  }

  /**
   * 更新会议室状态
   *
   * @param id 会议室ID
   * @param status 状态
   * @return 无返回
   */
  @Operation(summary = "更新会议室状态")
  @PutMapping("/{id}/status")
  @RequirePermission("admin:meeting:manage")
  @OperationLog(module = "会议室管理", action = "更新会议室状态")
  public Result<Void> updateRoomStatus(
      @PathVariable final Long id, @RequestParam final String status) {
    meetingRoomAppService.updateRoomStatus(id, status);
    return Result.success();
  }

  // ==================== 会议预约 ====================

  /**
   * 分页查询会议预约
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Operation(summary = "分页查询会议预约")
  @GetMapping("/bookings")
  public Result<PageResult<MeetingBookingDTO>> listBookings(final MeetingBookingQueryDTO query) {
    return Result.success(meetingRoomAppService.listBookings(query));
  }

  /**
   * 预约会议室
   *
   * @param command 预约命令
   * @return 预约信息
   */
  @Operation(summary = "预约会议室")
  @PostMapping("/bookings")
  @OperationLog(module = "会议室管理", action = "预约会议室")
  public Result<MeetingBookingDTO> bookMeeting(@RequestBody final BookMeetingCommand command) {
    return Result.success(meetingRoomAppService.bookMeeting(command));
  }

  /**
   * 取消会议预约
   *
   * @param id 预约ID
   * @return 无返回
   */
  @Operation(summary = "取消会议预约")
  @PostMapping("/bookings/{id}/cancel")
  @OperationLog(module = "会议室管理", action = "取消会议预约")
  public Result<Void> cancelBooking(@PathVariable final Long id) {
    meetingRoomAppService.cancelBooking(id);
    return Result.success();
  }

  /**
   * 获取会议室某日预约情况
   *
   * @param roomId 会议室ID
   * @param date 日期
   * @return 预约列表
   */
  @Operation(summary = "获取会议室某日预约情况")
  @GetMapping("/{roomId}/bookings/day")
  public Result<List<MeetingBookingDTO>> getRoomDayBookings(
      @PathVariable final Long roomId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate date) {
    return Result.success(meetingRoomAppService.getRoomDayBookings(roomId, date));
  }

  /**
   * 获取我的会议预约
   *
   * @return 预约列表
   */
  @Operation(summary = "获取我的会议预约")
  @GetMapping("/bookings/my")
  public Result<List<MeetingBookingDTO>> getMyBookings() {
    return Result.success(meetingRoomAppService.getMyBookings());
  }

  /**
   * 获取会议室日程视图（M8-024）
   *
   * @param roomId 会议室ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 预约列表
   */
  @Operation(summary = "获取会议室日程视图（M8-024）")
  @GetMapping("/{roomId}/schedule")
  public Result<List<MeetingBookingDTO>> getRoomSchedule(
      @PathVariable final Long roomId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate) {
    return Result.success(meetingRoomAppService.getRoomSchedule(roomId, startDate, endDate));
  }

  /**
   * 获取所有会议室日程视图（M8-024）
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 日程视图
   */
  @Operation(summary = "获取所有会议室日程视图（M8-024）")
  @GetMapping("/schedule/all")
  public Result<Map<Long, List<MeetingBookingDTO>>> getAllRoomsSchedule(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate) {
    return Result.success(meetingRoomAppService.getAllRoomsSchedule(startDate, endDate));
  }
}
