package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 系统通知DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationDTO extends BaseDTO {
    private Long id;
    private String title;
    private String content;
    private String type;
    private String typeName;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String businessType;
    private Long businessId;
    private LocalDateTime createdAt;
}
