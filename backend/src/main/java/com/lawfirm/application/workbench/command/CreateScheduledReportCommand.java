package com.lawfirm.application.workbench.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建定时报表任务命令
 */
@Data
public class CreateScheduledReportCommand {
    
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    
    private String description;
    
    @NotNull(message = "报表模板不能为空")
    private Long templateId;
    
    @NotBlank(message = "调度类型不能为空")
    private String scheduleType;
    
    /**
     * Cron表达式（scheduleType=CRON时必填）
     */
    private String cronExpression;
    
    /**
     * 执行时间 HH:mm（DAILY/WEEKLY/MONTHLY时必填）
     */
    private String executeTime;
    
    /**
     * 执行星期几 1-7（WEEKLY时必填）
     */
    private Integer executeDayOfWeek;
    
    /**
     * 执行日期 1-31（MONTHLY时必填）
     */
    private Integer executeDayOfMonth;
    
    /**
     * 报表参数
     */
    private Map<String, Object> reportParameters;
    
    /**
     * 输出格式：EXCEL, PDF
     */
    private String outputFormat = "EXCEL";
    
    /**
     * 是否启用通知
     */
    private Boolean notifyEnabled = false;
    
    /**
     * 通知邮箱列表
     */
    private List<String> notifyEmails;
    
    /**
     * 通知用户ID列表
     */
    private List<Long> notifyUserIds;
}
