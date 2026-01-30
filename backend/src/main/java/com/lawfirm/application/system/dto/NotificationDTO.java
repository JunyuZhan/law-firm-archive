package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.BaseDTO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统通知DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationDTO extends BaseDTO {
  /** 标题. */
  private String title;

  /** 内容. */
  private String content;

  /** 类型. */
  private String type;

  /** 类型名称. */
  private String typeName;

  /** 发送人ID. */
  private Long senderId;

  /** 发送人姓名. */
  private String senderName;

  /** 接收人ID. */
  private Long receiverId;

  /** 是否已读. */
  private Boolean isRead;

  /** 阅读时间. */
  private LocalDateTime readAt;

  /** 业务类型. */
  private String businessType;

  /** 业务ID. */
  private Long businessId;
}
