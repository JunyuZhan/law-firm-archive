package com.lawfirm.domain.workbench.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** 定时报表任务实体 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("workbench_scheduled_report")
public class ScheduledReport extends BaseEntity {

  /** 任务编号 */
  private String taskNo;

  /** 任务名称 */
  private String taskName;

  /** 任务描述 */
  private String description;

  /** 关联的报表模板ID */
  private Long templateId;

  /** 调度类型：DAILY-每日, WEEKLY-每周, MONTHLY-每月, CRON-自定义 */
  private String scheduleType;

  /** Cron表达式 */
  private String cronExpression;

  /** 执行时间 HH:mm */
  private String executeTime;

  /** 执行星期几 1-7 */
  private Integer executeDayOfWeek;

  /** 执行日期 1-31 */
  private Integer executeDayOfMonth;

  /** 报表参数（JSON格式） */
  private String reportParameters;

  /** 输出格式：EXCEL, PDF */
  private String outputFormat;

  /** 是否启用通知 */
  private Boolean notifyEnabled;

  /** 通知邮箱列表 */
  private String notifyEmails;

  /** 通知用户ID列表 */
  private String notifyUserIds;

  /** 状态：ACTIVE-启用, PAUSED-暂停, INACTIVE-停用 */
  private String status;

  /** 上次执行时间 */
  private LocalDateTime lastExecuteTime;

  /** 上次执行状态 */
  private String lastExecuteStatus;

  /** 下次执行时间 */
  private LocalDateTime nextExecuteTime;

  /** 总执行次数 */
  private Integer totalExecuteCount;

  /** 成功次数 */
  private Integer successCount;

  /** 失败次数 */
  private Integer failCount;

  /** 创建人姓名 */
  private String createdByName;
}
