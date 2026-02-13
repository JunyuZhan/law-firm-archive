package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 操作日志查询DTO */
@Data
public class OperationLogQueryDTO {

  /** 操作模块 */
  private String module;

  /** 操作类型 */
  private String operationType;

  /** 操作用户名（模糊查询） */
  private String userName;

  /** 操作用户ID */
  private Long userId;

  /** 状态：SUCCESS/FAIL */
  private String status;

  /** IP地址（模糊查询） */
  private String ipAddress;

  /** 请求URL（模糊查询） */
  private String requestUrl;

  /** 开始时间 */
  private LocalDateTime startTime;

  /** 结束时间 */
  private LocalDateTime endTime;

  /** 最小执行时长(ms) */
  private Long minExecutionTime;

  /** 页码 */
  private Integer pageNum = 1;

  /** 每页大小 */
  private Integer pageSize = 20;
}
