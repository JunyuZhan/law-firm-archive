package com.lawfirm.application.workbench.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/** 定时报表任务 DTO. */
@Data
public class ScheduledReportDTO {
  /** 主键ID. */
  private Long id;

  /** 任务编号. */
  private String taskNo;

  /** 任务名称. */
  private String taskName;

  /** 描述. */
  private String description;

  /** 模板ID. */
  private Long templateId;

  /** 模板名称. */
  private String templateName;

  /** 调度类型. */
  private String scheduleType;

  /** 调度类型名称. */
  private String scheduleTypeName;

  /** Cron表达式. */
  private String cronExpression;

  /** 执行时间. */
  private String executeTime;

  /** 执行星期几. */
  private Integer executeDayOfWeek;

  /** 执行日期（每月第几天）. */
  private Integer executeDayOfMonth;

  /** 调度描述（如：每天 08:00 执行）. */
  private String scheduleDescription;

  /** 报表参数. */
  private Map<String, Object> reportParameters;

  /** 输出格式. */
  private String outputFormat;

  /** 是否启用通知. */
  private Boolean notifyEnabled;

  /** 通知邮箱列表. */
  private List<String> notifyEmails;

  /** 通知用户ID列表. */
  private List<Long> notifyUserIds;

  /** 状态. */
  private String status;

  /** 状态名称. */
  private String statusName;

  /** 最后执行时间. */
  private LocalDateTime lastExecuteTime;

  /** 最后执行状态. */
  private String lastExecuteStatus;

  /** 最后执行状态名称. */
  private String lastExecuteStatusName;

  /** 下次执行时间. */
  private LocalDateTime nextExecuteTime;

  /** 总执行次数. */
  private Integer totalExecuteCount;

  /** 成功次数. */
  private Integer successCount;

  /** 失败次数. */
  private Integer failCount;

  /** 创建人ID. */
  private Long createdBy;

  /** 创建人名称. */
  private String createdByName;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
