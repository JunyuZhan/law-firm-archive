package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.command.CreateMeetingRecordCommand;
import com.lawfirm.application.admin.dto.MeetingRecordDTO;
import com.lawfirm.application.admin.service.MeetingRecordAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会议记录接口（M8-023） */
@Tag(name = "会议记录", description = "会议记录管理")
@RestController
@RequestMapping("/admin/meeting-records")
@RequiredArgsConstructor
public class MeetingRecordController {

  /** 默认查询天数：30天 */
  private static final int DEFAULT_QUERY_DAYS = 30;

  /** 会议记录应用服务 */
  private final MeetingRecordAppService recordAppService;

  /**
   * 查询会议记录列表
   *
   * @return 会议记录列表
   */
  @Operation(summary = "查询会议记录列表")
  @GetMapping
  @RequirePermission("admin:meeting:view")
  public Result<List<MeetingRecordDTO>> listRecords() {
    // 返回最近30天的会议记录
    LocalDate endDate = LocalDate.now();
    LocalDate startDate = endDate.minusDays(DEFAULT_QUERY_DAYS);
    return Result.success(recordAppService.getRecordsByDateRange(startDate, endDate));
  }

  /**
   * 创建会议记录
   *
   * @param command 创建命令
   * @return 会议记录
   */
  @Operation(summary = "创建会议记录")
  @PostMapping
  @RequirePermission("admin:meeting:record")
  @OperationLog(module = "会议记录", action = "创建会议记录")
  public Result<MeetingRecordDTO> createRecord(
      @RequestBody @Valid final CreateMeetingRecordCommand command) {
    return Result.success(recordAppService.createRecord(command));
  }

  /**
   * 根据预约创建会议记录
   *
   * @param bookingId 预约ID
   * @param command 创建命令
   * @return 会议记录
   */
  @Operation(summary = "根据预约创建会议记录")
  @PostMapping("/from-booking/{bookingId}")
  @RequirePermission("admin:meeting:record")
  @OperationLog(module = "会议记录", action = "根据预约创建会议记录")
  public Result<MeetingRecordDTO> createRecordFromBooking(
      @PathVariable final Long bookingId,
      @RequestBody @Valid final CreateMeetingRecordCommand command) {
    return Result.success(recordAppService.createRecordFromBooking(bookingId, command));
  }

  /**
   * 获取会议记录详情
   *
   * @param id 会议记录ID
   * @return 会议记录详情
   */
  @Operation(summary = "获取会议记录详情")
  @GetMapping("/{id}")
  @RequirePermission("admin:meeting:view")
  public Result<MeetingRecordDTO> getRecord(@PathVariable final Long id) {
    return Result.success(recordAppService.getRecordById(id));
  }

  /**
   * 查询会议室的会议记录
   *
   * @param roomId 会议室ID
   * @return 会议记录列表
   */
  @Operation(summary = "查询会议室的会议记录")
  @GetMapping("/room/{roomId}")
  @RequirePermission("admin:meeting:view")
  public Result<List<MeetingRecordDTO>> getRecordsByRoom(@PathVariable final Long roomId) {
    return Result.success(recordAppService.getRecordsByRoom(roomId));
  }

  /**
   * 查询指定日期范围的会议记录
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 会议记录列表
   */
  @Operation(summary = "查询指定日期范围的会议记录")
  @GetMapping("/range")
  @RequirePermission("admin:meeting:view")
  public Result<List<MeetingRecordDTO>> getRecordsByDateRange(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate) {
    return Result.success(recordAppService.getRecordsByDateRange(startDate, endDate));
  }
}
