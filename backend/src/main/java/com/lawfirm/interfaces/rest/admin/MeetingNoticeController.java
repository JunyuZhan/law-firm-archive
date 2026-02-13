package com.lawfirm.interfaces.rest.admin;

import com.lawfirm.application.admin.service.MeetingNoticeAppService;
import com.lawfirm.common.annotation.OperationLog;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会议通知接口（M8-022） */
@Tag(name = "会议通知", description = "会议通知发送")
@RestController
@RequestMapping("/admin/meeting-notices")
@RequiredArgsConstructor
public class MeetingNoticeController {

  /** 会议通知应用服务 */
  private final MeetingNoticeAppService noticeAppService;

  /**
   * 发送会议通知
   *
   * @param bookingId 预约ID
   * @return 无返回
   */
  @Operation(summary = "发送会议通知")
  @PostMapping("/{bookingId}/send")
  @RequirePermission("admin:meeting:notice")
  @OperationLog(module = "会议通知", action = "发送会议通知")
  public Result<Void> sendNotice(@PathVariable final Long bookingId) {
    noticeAppService.sendMeetingNotice(bookingId);
    return Result.success();
  }

  /**
   * 批量发送即将开始的会议通知
   *
   * @param minutesBefore 提前分钟数
   * @return 发送数量
   */
  @Operation(summary = "批量发送即将开始的会议通知")
  @PostMapping("/send-upcoming")
  @RequirePermission("admin:meeting:notice")
  @OperationLog(module = "会议通知", action = "批量发送会议通知")
  public Result<Integer> sendUpcomingNotices(
      @RequestParam(defaultValue = "30") final int minutesBefore) {
    int sentCount = noticeAppService.sendUpcomingMeetingNotices(minutesBefore);
    return Result.success(sentCount);
  }
}
