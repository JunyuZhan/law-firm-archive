package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NotificationAppService 单元测试
 *
 * <p>测试系统通知应用服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationAppService 系统通知服务测试")
class NotificationAppServiceTest {

  @Mock private NotificationRepository notificationRepository;

  @Mock private NotificationMapper notificationMapper;

  @Mock private WecomNotificationChannel wecomChannel;

  @InjectMocks private NotificationAppService service;

  private static final Long TEST_USER_ID = 1L;

  // ========== 查询测试 ==========

  @Test
  @DisplayName("应该分页查询我的通知")
  void listMyNotifications_shouldReturnPagedNotifications() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      Notification notification = createTestNotification(1L, "测试通知", "测试内容");
      Page<Notification> page = new Page<>(1, 20);
      page.setRecords(List.of(notification));
      page.setTotal(1);

      when(notificationMapper.selectByReceiver(any(), eq(TEST_USER_ID), any(), any()))
          .thenReturn(page);

      NotificationQueryDTO query = new NotificationQueryDTO();
      query.setPageNum(1);
      query.setPageSize(20);

      PageResult<NotificationDTO> result = service.listMyNotifications(query);

      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getTitle()).isEqualTo("测试通知");
      assertThat(result.getTotal()).isEqualTo(1);
    }
  }

  @Test
  @DisplayName("应该使用默认分页参数")
  void listMyNotifications_shouldUseDefaultPagination() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      Page<Notification> page = new Page<>(1, 20);
      page.setRecords(List.of());
      page.setTotal(0);

      when(notificationMapper.selectByReceiver(any(), eq(TEST_USER_ID), any(), any()))
          .thenReturn(page);

      PageResult<NotificationDTO> result = service.listMyNotifications(null);

      assertThat(result.getRecords()).isEmpty();
    }
  }

  @Test
  @DisplayName("应该获取未读通知数量")
  void getUnreadCount_shouldReturnCount() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      when(notificationMapper.countUnread(TEST_USER_ID)).thenReturn(5);

      int count = service.getUnreadCount();

      assertThat(count).isEqualTo(5);
    }
  }

  // ========== 标记已读测试 ==========

  @Test
  @DisplayName("应该标记通知为已读")
  void markAsRead_shouldMarkNotification() {
    when(notificationMapper.markAsRead(1L)).thenReturn(1);

    service.markAsRead(1L);

    verify(notificationMapper).markAsRead(1L);
  }

  @Test
  @DisplayName("应该标记所有通知为已读")
  void markAllAsRead_shouldMarkAllNotifications() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      when(notificationMapper.markAllAsRead(TEST_USER_ID)).thenReturn(10);

      service.markAllAsRead();

      verify(notificationMapper).markAllAsRead(TEST_USER_ID);
    }
  }

  // ========== 发送通知测试 ==========

  @Test
  @DisplayName("应该批量发送通知")
  void sendNotification_shouldSendToMultipleReceivers() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      SendNotificationCommand command = new SendNotificationCommand();
      command.setTitle("测试标题");
      command.setContent("测试内容");
      command.setType(Notification.TYPE_SYSTEM);
      command.setReceiverIds(List.of(1L, 2L, 3L));

      when(notificationRepository.saveBatch(anyList())).thenReturn(true);

      service.sendNotification(command);

      verify(notificationRepository, times(1)).saveBatch(anyList());
    }
  }

  @Test
  @DisplayName("接收人为空时不应该发送通知")
  void sendNotification_shouldNotSendWhenReceiversEmpty() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      SendNotificationCommand command = new SendNotificationCommand();
      command.setTitle("测试标题");
      command.setContent("测试内容");
      command.setReceiverIds(List.of());

      service.sendNotification(command);

      verify(notificationRepository, never()).saveBatch(anyList());
    }
  }

  @Test
  @DisplayName("接收人为null时不应该发送通知")
  void sendNotification_shouldNotSendWhenReceiversNull() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      SendNotificationCommand command = new SendNotificationCommand();
      command.setTitle("测试标题");
      command.setContent("测试内容");
      command.setReceiverIds(null);

      service.sendNotification(command);

      verify(notificationRepository, never()).saveBatch(anyList());
    }
  }

  @Test
  @DisplayName("应该发送系统通知")
  void sendSystemNotification_shouldSendSystemNotification() {
    when(notificationRepository.save(any(Notification.class))).thenReturn(true);

    service.sendSystemNotification(1L, "系统通知", "通知内容", "MATTER", 100L);

    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("应该发送带自定义类型的系统通知")
  void sendSystemNotificationWithType_shouldSendWithCustomType() {
    when(notificationRepository.save(any(Notification.class))).thenReturn(true);

    service.sendSystemNotificationWithType(
        1L, "任务通知", "新任务分配", Notification.TYPE_TASK, "TASK", 200L);

    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("应该发送紧急通知")
  void sendUrgentNotification_shouldSendUrgentNotification() {
    List<Long> receiverIds = List.of(1L, 2L);
    when(notificationRepository.save(any(Notification.class))).thenReturn(true);

    service.sendUrgentNotification(receiverIds, "紧急通知", "需要立即处理", "URGENT", 1L);

    // 验证每个接收人都收到了通知
    verify(notificationRepository, times(2)).save(any(Notification.class));
  }

  // ========== 企业微信测试 ==========

  @Test
  @DisplayName("应该检查企业微信是否启用")
  void isWecomEnabled_shouldReturnTrueWhenEnabled() {
    // 使用反射设置wecomChannel
    setWecomChannel(wecomChannel);
    when(wecomChannel.isEnabled()).thenReturn(true);

    boolean result = service.isWecomEnabled();

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("企业微信未配置时应该返回false")
  void isWecomEnabled_shouldReturnFalseWhenNotConfigured() {
    // wecomChannel保持为null（@Autowired(required = false)）
    boolean result = service.isWecomEnabled();

    assertThat(result).isFalse();
  }

  // ========== 删除测试 ==========

  @Test
  @DisplayName("应该删除通知")
  void deleteNotification_shouldDelete() {
    when(notificationMapper.deleteById(1L)).thenReturn(1);

    service.deleteNotification(1L);

    verify(notificationMapper).deleteById(1L);
  }

  @Test
  @DisplayName("应该批量删除已读通知")
  void deleteReadNotifications_shouldDeleteReadNotifications() {
    try (var mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

      when(notificationMapper.deleteReadNotifications(TEST_USER_ID)).thenReturn(10);

      service.deleteReadNotifications();

      verify(notificationMapper).deleteReadNotifications(TEST_USER_ID);
    }
  }

  // ========== 辅助方法 ==========

  private Notification createTestNotification(Long id, String title, String content) {
    return Notification.builder()
        .id(id)
        .title(title)
        .content(content)
        .type(Notification.TYPE_SYSTEM)
        .senderId(100L)
        .receiverId(TEST_USER_ID)
        .isRead(false)
        .build();
  }

  private void setWecomChannel(WecomNotificationChannel channel) {
    try {
      Field field = NotificationAppService.class.getDeclaredField("wecomChannel");
      field.setAccessible(true);
      field.set(service, channel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
