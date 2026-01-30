package com.lawfirm.application.workbench.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 定时报表执行记录 DTO. */
@Data
public class ScheduledReportLogDTO {
  /** 主键ID. */
  private Long id;

  /** 任务ID. */
  private Long taskId;

  /** 任务编号. */
  private String taskNo;

  /** 执行时间. */
  private LocalDateTime executeTime;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 报表ID. */
  private Long reportId;

  /** 文件URL. */
  private String fileUrl;

  /** 文件大小（字节）. */
  private Long fileSize;

  /** 文件大小显示. */
  private String fileSizeDisplay;

  /** 执行耗时（毫秒）. */
  private Long durationMs;

  /** 执行耗时显示. */
  private String durationDisplay;

  /** 错误信息. */
  private String errorMessage;

  /** 通知状态. */
  private String notifyStatus;

  /** 通知状态名称. */
  private String notifyStatusName;

  /** 通知结果. */
  private String notifyResult;

  /** 创建时间. */
  private LocalDateTime createdAt;
}
