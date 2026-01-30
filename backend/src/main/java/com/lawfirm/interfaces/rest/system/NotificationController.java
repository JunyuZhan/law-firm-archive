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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 系统通知接口 */
@Tag(name = "系统通知", description = "消息通知相关接口")
@RestController
@RequestMapping("/system/notification")
@RequiredArgsConstructor
public class NotificationController {

  /** 通知应用服务 */
  private final NotificationAppService notificationAppService;

  /**
   * 分页查询我的通知
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @param type 通知类型
   * @param isRead 是否已读
   * @return 通知分页结果
   */
  @Operation(summary = "分页查询我的通知")
  @GetMapping
  public Result<PageResult<NotificationDTO>> listMyNotifications(
      @RequestParam(required = false) final Integer pageNum,
      @RequestParam(required = false) final Integer pageSize,
      @RequestParam(required = false) final String type,
      @RequestParam(required = false) final Boolean isRead) {
    NotificationQueryDTO query = new NotificationQueryDTO();
    if (pageNum != null) {
      query.setPageNum(pageNum);
    }
    if (pageSize != null) {
      query.setPageSize(pageSize);
    }
    if (type != null) {
      query.setType(type);
    }
    if (isRead != null) {
      query.setIsRead(isRead);
    }
    return Result.success(notificationAppService.listMyNotifications(query));
  }

  /**
   * 获取未读数量
   *
   * @return 未读通知数量
   */
  @Operation(summary = "获取未读数量")
  @GetMapping("/unread-count")
  public Result<Integer> getUnreadCount() {
    return Result.success(notificationAppService.getUnreadCount());
  }

  /**
   * 标记为已读
   *
   * @param id 通知ID
   * @return 空结果
   */
  @Operation(summary = "标记为已读")
  @PostMapping("/{id}/read")
  public Result<Void> markAsRead(@PathVariable final Long id) {
    notificationAppService.markAsRead(id);
    return Result.success();
  }

  /**
   * 全部标记为已读
   *
   * @return 空结果
   */
  @Operation(summary = "全部标记为已读")
  @PostMapping("/read-all")
  public Result<Void> markAllAsRead() {
    notificationAppService.markAllAsRead();
    return Result.success();
  }

  /**
   * 发送通知
   *
   * @param command 发送通知命令
   * @return 空结果
   */
  @Operation(summary = "发送通知")
  @PostMapping("/send")
  @RequirePermission("sys:notification:send")
  public Result<Void> sendNotification(@RequestBody final SendNotificationCommand command) {
    notificationAppService.sendNotification(command);
    return Result.success();
  }

  /**
   * 删除通知
   *
   * @param id 通知ID
   * @return 空结果
   */
  @Operation(summary = "删除通知")
  @DeleteMapping("/{id}")
  public Result<Void> deleteNotification(@PathVariable final Long id) {
    notificationAppService.deleteNotification(id);
    return Result.success();
  }

  /**
   * 批量删除已读通知
   *
   * @return 空结果
   */
  @Operation(summary = "批量删除已读通知")
  @DeleteMapping("/read")
  public Result<Void> deleteReadNotifications() {
    notificationAppService.deleteReadNotifications();
    return Result.success();
  }
}
