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
import com.lawfirm.infrastructure.persistence.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统通知应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationAppService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    /**
     * 分页查询我的通知
     */
    public PageResult<NotificationDTO> listMyNotifications(NotificationQueryDTO query) {
        Long userId = SecurityUtils.getUserId();
        IPage<Notification> page = notificationMapper.selectByReceiver(
                new Page<>(query.getPageNum(), query.getPageSize()),
                userId,
                query.getType(),
                query.getIsRead()
        );

        List<NotificationDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取未读数量
     */
    public int getUnreadCount() {
        Long userId = SecurityUtils.getUserId();
        return notificationMapper.countUnread(userId);
    }

    /**
     * 标记为已读
     */
    @Transactional
    public void markAsRead(Long id) {
        notificationMapper.markAsRead(id);
    }

    /**
     * 全部标记为已读
     */
    @Transactional
    public void markAllAsRead() {
        Long userId = SecurityUtils.getUserId();
        notificationMapper.markAllAsRead(userId);
    }

    /**
     * 发送通知
     */
    @Transactional
    public void sendNotification(SendNotificationCommand command) {
        Long senderId = SecurityUtils.getUserId();
        
        for (Long receiverId : command.getReceiverIds()) {
            Notification notification = Notification.builder()
                    .title(command.getTitle())
                    .content(command.getContent())
                    .type(command.getType() != null ? command.getType() : Notification.TYPE_SYSTEM)
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .isRead(false)
                    .businessType(command.getBusinessType())
                    .businessId(command.getBusinessId())
                    .build();
            notificationRepository.save(notification);
        }
        
        log.info("通知发送成功: title={}, receivers={}", command.getTitle(), command.getReceiverIds().size());
    }

    /**
     * 发送系统通知（内部调用）
     */
    @Transactional
    public void sendSystemNotification(Long receiverId, String title, String content, String businessType, Long businessId) {
        Notification notification = Notification.builder()
                .title(title)
                .content(content)
                .type(Notification.TYPE_SYSTEM)
                .receiverId(receiverId)
                .isRead(false)
                .businessType(businessType)
                .businessId(businessId)
                .build();
        notificationRepository.save(notification);
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long id) {
        notificationMapper.deleteById(id);
    }

    private String getTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case Notification.TYPE_SYSTEM -> "系统通知";
            case Notification.TYPE_APPROVAL -> "审批通知";
            case Notification.TYPE_TASK -> "任务通知";
            case Notification.TYPE_REMINDER -> "提醒通知";
            default -> type;
        };
    }

    private NotificationDTO toDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
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
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
