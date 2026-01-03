package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.service.MeetingNoticeAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 会议通知接口（M8-022）
 */
@Tag(name = "会议通知", description = "会议通知发送")
@RestController
@RequestMapping("/admin/meeting-notices")
@RequiredArgsConstructor
public class MeetingNoticeController {

    private final MeetingNoticeAppService noticeAppService;

    @Operation(summary = "发送会议通知")
    @PostMapping("/{bookingId}/send")
    @RequirePermission("admin:meeting:notice")
    @OperationLog(module = "会议通知", action = "发送会议通知")
    public Result<Void> sendNotice(@PathVariable Long bookingId) {
        noticeAppService.sendMeetingNotice(bookingId);
        return Result.success();
    }

    @Operation(summary = "批量发送即将开始的会议通知")
    @PostMapping("/send-upcoming")
    @RequirePermission("admin:meeting:notice")
    @OperationLog(module = "会议通知", action = "批量发送会议通知")
    public Result<Integer> sendUpcomingNotices(@RequestParam(defaultValue = "30") int minutesBefore) {
        int sentCount = noticeAppService.sendUpcomingMeetingNotices(minutesBefore);
        return Result.success(sentCount);
    }
}

