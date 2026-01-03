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
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 会议记录接口（M8-023）
 */
@Tag(name = "会议记录", description = "会议记录管理")
@RestController
@RequestMapping("/admin/meeting-records")
@RequiredArgsConstructor
public class MeetingRecordController {

    private final MeetingRecordAppService recordAppService;

    @Operation(summary = "创建会议记录")
    @PostMapping
    @RequirePermission("admin:meeting:record")
    @OperationLog(module = "会议记录", action = "创建会议记录")
    public Result<MeetingRecordDTO> createRecord(@RequestBody @Valid CreateMeetingRecordCommand command) {
        return Result.success(recordAppService.createRecord(command));
    }

    @Operation(summary = "根据预约创建会议记录")
    @PostMapping("/from-booking/{bookingId}")
    @RequirePermission("admin:meeting:record")
    @OperationLog(module = "会议记录", action = "根据预约创建会议记录")
    public Result<MeetingRecordDTO> createRecordFromBooking(
            @PathVariable Long bookingId,
            @RequestBody @Valid CreateMeetingRecordCommand command) {
        return Result.success(recordAppService.createRecordFromBooking(bookingId, command));
    }

    @Operation(summary = "获取会议记录详情")
    @GetMapping("/{id}")
    @RequirePermission("admin:meeting:view")
    public Result<MeetingRecordDTO> getRecord(@PathVariable Long id) {
        return Result.success(recordAppService.getRecordById(id));
    }

    @Operation(summary = "查询会议室的会议记录")
    @GetMapping("/room/{roomId}")
    @RequirePermission("admin:meeting:view")
    public Result<List<MeetingRecordDTO>> getRecordsByRoom(@PathVariable Long roomId) {
        return Result.success(recordAppService.getRecordsByRoom(roomId));
    }

    @Operation(summary = "查询指定日期范围的会议记录")
    @GetMapping("/range")
    @RequirePermission("admin:meeting:view")
    public Result<List<MeetingRecordDTO>> getRecordsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(recordAppService.getRecordsByDateRange(startDate, endDate));
    }
}

