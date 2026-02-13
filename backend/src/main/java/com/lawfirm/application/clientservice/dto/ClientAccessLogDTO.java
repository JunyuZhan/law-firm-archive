package com.lawfirm.application.clientservice.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 客户访问日志 DTO */
@Data
public class ClientAccessLogDTO {

  /** 日志ID */
  private Long id;

  /** 项目ID */
  private Long matterId;

  /** 客户ID */
  private Long clientId;

  /** 访问时间 */
  private LocalDateTime accessTime;

  /** IP地址 */
  private String ipAddress;

  /** 用户代理 */
  private String userAgent;

  /** 事件类型 */
  private String eventType;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
