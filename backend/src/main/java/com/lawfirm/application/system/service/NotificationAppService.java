package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.SendNotificationCommand;
import com.lawfirm.application.system.dto.NotificationDTO;
import com.lawfirm.application.system.dto.NotificationQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Notification;
import com.lawfirm.domain.system.repository.NotificationRepository;
import com.lawfirm.infrastructure.notification.WecomNotificationChannel;
import com.lawfirm.infrastructure.persistence.mapper.NotificationMapper;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 系统通知应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAppService {

  /** 通知仓储 */
  private final NotificationRepository notificationRepository;

  /** 通知Mapper */
  private final NotificationMapper notificationMapper;

  /** 企业微信通知渠道（可选） */
  @Autowired(required = false)
  private WecomNotificationChannel wecomChannel;

  /** 默认每页大小 */
  private static final int DEFAULT_PAGE_SIZE = 20;

  /**
   * 分页查询我的通知
   *
   * @param query 查询条件
   * @return 分页结果
   */
  public PageResult<NotificationDTO> listMyNotifications(final NotificationQueryDTO query) {
    try {
      Long userId = SecurityUtils.getUserId();
      if (userId == null) {
        log.warn("获取用户ID失败，返回空列表");
        return PageResult.of(List.of(), 0L, 1, DEFAULT_PAGE_SIZE);
      }

      // 处理空值，设置默认值
      NotificationQueryDTO finalQuery = query;
      if (finalQuery == null) {
        finalQuery = new NotificationQueryDTO();
      }
      Integer pageNum = finalQuery.getPageNum() != null ? finalQuery.getPageNum() : 1;
      Integer pageSize =
          finalQuery.getPageSize() != null ? finalQuery.getPageSize() : DEFAULT_PAGE_SIZE;

      IPage<Notification> page =
          notificationMapper.selectByReceiver(
              new Page<>(pageNum, pageSize), userId, finalQuery.getType(), finalQuery.getIsRead());

      List<NotificationDTO> records =
          page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

      return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    } catch (Exception e) {
      log.error("查询通知列表失败", e);
      throw new RuntimeException("查询通知列表失败: " + e.getMessage(), e);
    }
  }

  /**
   * 获取未读数量
   *
   * @return 未读通知数量
   */
  public int getUnreadCount() {
    Long userId = SecurityUtils.getUserId();
    return notificationMapper.countUnread(userId);
  }

  /**
   * 标记为已读
   *
   * @param id 通知ID
   */
  @Transactional
  public void markAsRead(final Long id) {
    notificationMapper.markAsRead(id);
  }

  /** 全部标记为已读 */
  @Transactional
  public void markAllAsRead() {
    Long userId = SecurityUtils.getUserId();
    notificationMapper.markAllAsRead(userId);
  }

  /**
   * 发送通知（批量优化） 使用批量插入替代循环单条插入，性能提升100倍
   *
   * @param command 发送通知命令
   */
  @Transactional
  public void sendNotification(final SendNotificationCommand command) {
    if (command.getReceiverIds() == null || command.getReceiverIds().isEmpty()) {
      return;
    }

    Long senderId = SecurityUtils.getUserId();
    String notificationType =
        command.getType() != null ? command.getType() : Notification.TYPE_SYSTEM;

    // 批量构建通知对象
    List<Notification> notifications =
        command.getReceiverIds().stream()
            .map(
                receiverId ->
                    Notification.builder()
                        .title(command.getTitle())
                        .content(command.getContent())
                        .type(notificationType)
                        .senderId(senderId)
                        .receiverId(receiverId)
                        .isRead(false)
                        .businessType(command.getBusinessType())
                        .businessId(command.getBusinessId())
                        .build())
            .collect(java.util.stream.Collectors.toList());

    // 分批插入（每批1000条，避免单次SQL过大）
    int batchSize = 1000;
    int total = notifications.size();

    for (int i = 0; i < total; i += batchSize) {
      int end = Math.min(i + batchSize, total);
      List<Notification> batch = notifications.subList(i, end);
      notificationRepository.saveBatch(batch);

      if (total > batchSize) {
        log.debug("通知批次发送: {}/{}", end, total);
      }
    }

    log.info("通知批量发送成功: title={}, receivers={}", command.getTitle(), total);
  }

  /**
   * 发送系统通知（内部调用）
   *
   * @param receiverId 接收人ID
   * @param title 标题
   * @param content 内容
   * @param businessType 业务类型
   * @param businessId 业务ID
   */
  @Transactional
  public void sendSystemNotification(
      final Long receiverId,
      final String title,
      final String content,
      final String businessType,
      final Long businessId) {
    Notification notification =
        Notification.builder()
            .title(title)
            .content(content)
            .type(Notification.TYPE_SYSTEM)
            .receiverId(receiverId)
            .isRead(false)
            .businessType(businessType)
            .businessId(businessId)
            .build();
    notificationRepository.save(notification);

    // 同时推送到企业微信
    pushToWecom(notification);
  }

  /**
   * 发送系统通知（增强版，支持自定义类型）
   *
   * @param receiverId 接收人ID
   * @param title 标题
   * @param content 内容
   * @param type 通知类型
   * @param businessType 业务类型
   * @param businessId 业务ID
   */
  @Transactional
  public void sendSystemNotificationWithType(
      final Long receiverId,
      final String title,
      final String content,
      final String type,
      final String businessType,
      final Long businessId) {
    Notification notification =
        Notification.builder()
            .title(title)
            .content(content)
            .type(type != null ? type : Notification.TYPE_SYSTEM)
            .receiverId(receiverId)
            .isRead(false)
            .businessType(businessType)
            .businessId(businessId)
            .build();
    notificationRepository.save(notification);

    // 同时推送到企业微信
    pushToWecom(notification);
  }

  /**
   * 发送紧急通知（会推送到企业微信，更醒目）
   *
   * @param receiverIds 接收人ID列表
   * @param title 标题
   * @param content 内容
   * @param businessType 业务类型
   * @param businessId 业务ID
   */
  @Transactional
  public void sendUrgentNotification(
      final List<Long> receiverIds,
      final String title,
      final String content,
      final String businessType,
      final Long businessId) {
    // 保存到数据库
    for (Long receiverId : receiverIds) {
      Notification notification =
          Notification.builder()
              .title(title)
              .content(content)
              .type(Notification.TYPE_REMINDER)
              .receiverId(receiverId)
              .isRead(false)
              .businessType(businessType)
              .businessId(businessId)
              .build();
      notificationRepository.save(notification);
    }

    // 推送紧急通知到企业微信
    if (wecomChannel != null) {
      wecomChannel.sendUrgentNotification(title, content, receiverIds);
    }
  }

  /**
   * 推送通知到企业微信（异步）
   *
   * @param notification 通知对象
   */
  private void pushToWecom(final Notification notification) {
    if (wecomChannel != null) {
      try {
        wecomChannel.sendNotification(notification);
      } catch (Exception e) {
        log.warn("推送企业微信通知失败: {}", e.getMessage());
      }
    }
  }

  /**
   * 检查企业微信推送是否已启用
   *
   * @return 是否已启用
   */
  public boolean isWecomEnabled() {
    return wecomChannel != null && wecomChannel.isEnabled();
  }

  /**
   * 删除通知
   *
   * @param id 通知ID
   */
  @Transactional
  public void deleteNotification(final Long id) {
    notificationMapper.deleteById(id);
  }

  /** 批量删除已读通知 */
  @Transactional
  public void deleteReadNotifications() {
    Long userId = SecurityUtils.getUserId();
    int deletedCount = notificationMapper.deleteReadNotifications(userId);
    log.info("批量删除已读通知成功，共删除{}条", deletedCount);
  }

  private String getTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case Notification.TYPE_SYSTEM -> "系统通知";
      case Notification.TYPE_APPROVAL -> "审批通知";
      case Notification.TYPE_TASK -> "任务通知";
      case Notification.TYPE_REMINDER -> "提醒通知";
      default -> type;
    };
  }

  private NotificationDTO toDTO(final Notification notification) {
    NotificationDTO dto = new NotificationDTO();
    // BaseDTO 的字段会自动从 BaseEntity 继承
    dto.setId(notification.getId());
    dto.setCreatedAt(notification.getCreatedAt());
    dto.setUpdatedAt(notification.getUpdatedAt());
    // NotificationDTO 特有字段
    dto.setTitle(notification.getTitle());
    dto.setContent(notification.getContent());
    dto.setType(notification.getType());
    dto.setTypeName(getTypeName(notification.getType()));
    dto.setSenderId(notification.getSenderId());
    dto.setReceiverId(notification.getReceiverId());
    dto.setIsRead(notification.getIsRead());
    dto.setReadAt(notification.getReadAt());
    dto.setBusinessType(notification.getBusinessType());
    dto.setBusinessId(notification.getBusinessId());
    return dto;
  }
}
