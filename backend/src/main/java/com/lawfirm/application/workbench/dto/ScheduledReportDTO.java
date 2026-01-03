package com.lawfirm.application.workbench.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 定时报表任务 DTO
 */
@Data
public class ScheduledReportDTO {
    private Long id;
    private String taskNo;
    private String taskName;
    private String description;
    
    private Long templateId;
    private String templateName;
    
    private String scheduleType;
    private String scheduleTypeName;
    private String cronExpression;
    private String executeTime;
    private Integer executeDayOfWeek;
    private Integer executeDayOfMonth;
    
    /**
     * 调度描述（如：每天 08:00 执行）
     */
    private String scheduleDescription;
    
    private Map<String, Object> reportParameters;
    private String outputFormat;
    
    private Boolean notifyEnabled;
    private List<String> notifyEmails;
    private List<Long> notifyUserIds;
    
    private String status;
    private String statusName;
    
    private LocalDateTime lastExecuteTime;
    private String lastExecuteStatus;
    private String lastExecuteStatusName;
    private LocalDateTime nextExecuteTime;
    
    private Integer totalExecuteCount;
    private Integer successCount;
    private Integer failCount;
    
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
