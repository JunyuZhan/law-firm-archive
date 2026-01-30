package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 系统通知实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notification")
public class Notification extends BaseEntity {

  /** 通知标题. */
  private String title;

  /** 通知内容. */
  private String content;

  /** 类型. */
  private String type;

  /** 发送者ID. */
  private Long senderId;

  /** 接收者ID. */
  private Long receiverId;

  /** 是否已读. */
  private Boolean isRead;

  /** 阅读时间. */
  private LocalDateTime readAt;

  /** 关联业务类型. */
  private String businessType;

  /** 关联业务ID. */
  private Long businessId;

  // 类型常量
  /** 类型：系统通知. */
  public static final String TYPE_SYSTEM = "SYSTEM";

  /** 类型：审批通知. */
  public static final String TYPE_APPROVAL = "APPROVAL";

  /** 类型：任务通知. */
  public static final String TYPE_TASK = "TASK";

  /** 类型：提醒通知. */
  public static final String TYPE_REMINDER = "REMINDER";
}
