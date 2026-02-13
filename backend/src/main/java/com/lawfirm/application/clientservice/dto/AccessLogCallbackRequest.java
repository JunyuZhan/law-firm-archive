package com.lawfirm.application.clientservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;

/** 访问日志回调请求（客户服务系统回调） */
@Data
public class AccessLogCallbackRequest {

  /** 律所系统的项目ID */
  @NotNull(message = "项目ID不能为空")
  private Long matterId;

  /** 客户ID */
  @NotNull(message = "客户ID不能为空")
  private Long clientId;

  /** 访问时间 */
  @NotNull(message = "访问时间不能为空")
  private LocalDateTime accessTime;

  /** IP地址 */
  private String ipAddress;

  /** 用户代理 */
  private String userAgent;

  /** 事件类型（固定值：ACCESS） */
  @NotNull(message = "事件类型不能为空")
  private String eventType;
}
