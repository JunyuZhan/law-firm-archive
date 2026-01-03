package com.lawfirm.interfaces.rest.system;

import com.lawfirm.application.system.command.SendNotificationCommand;
import com.lawfirm.application.system.dto.NotificationDTO;
import com.lawfirm.application.system.dto.NotificationQueryDTO;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.common.annotation.RequirePermission;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统通知接口
 */
@Tag(name = "系统通知", description = "消息通知相关接口")
@RestController
@RequestMapping("/system/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationAppService notificationAppService;

    @Operation(summary = "分页查询我的通知")
    @GetMapping
    public Result<PageResult<NotificationDTO>> listMyNotifications(NotificationQueryDTO query) {
        return Result.success(notificationAppService.listMyNotifications(query));
    }

    @Operation(summary = "获取未读数量")
    @GetMapping("/unread-count")
    public Result<Integer> getUnreadCount() {
        return Result.success(notificationAppService.getUnreadCount());
    }

    @Operation(summary = "标记为已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        notificationAppService.markAsRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记为已读")
    @PostMapping("/read-all")
    public Result<Void> markAllAsRead() {
        notificationAppService.markAllAsRead();
        return Result.success();
    }

    @Operation(summary = "发送通知")
    @PostMapping("/send")
    @RequirePermission("sys:notification:send")
    public Result<Void> sendNotification(@RequestBody SendNotificationCommand command) {
        notificationAppService.sendNotification(command);
        return Result.success();
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        notificationAppService.deleteNotification(id);
        return Result.success();
    }
}
