package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 操作日志DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogDTO {

  /** 日志ID */
  private Long id;

  /** 操作用户ID */
  private Long userId;

  /** 操作用户名 */
  private String userName;

  /** 操作模块 */
  private String module;

  /** 操作类型 */
  private String operationType;

  /** 操作描述 */
  private String description;

  /** 请求方法 */
  private String method;

  /** 请求URL */
  private String requestUrl;

  /** 请求方式 */
  private String requestMethod;

  /** 请求参数 */
  private String requestParams;

  /** IP地址 */
  private String ipAddress;

  /** 执行时长(ms) */
  private Long executionTime;

  /** 状态 */
  private String status;

  /** 状态名称 */
  private String statusName;

  /** 错误信息 */
  private String errorMessage;

  /** 创建时间 */
  private LocalDateTime createdAt;
}
