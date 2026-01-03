package com.lawfirm.application.workbench.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 定时报表执行记录 DTO
 */
@Data
public class ScheduledReportLogDTO {
    private Long id;
    private Long taskId;
    private String taskNo;
    
    private LocalDateTime executeTime;
    private String status;
    private String statusName;
    
    private Long reportId;
    private String fileUrl;
    private Long fileSize;
    private String fileSizeDisplay;
    
    private Long durationMs;
    private String durationDisplay;
    
    private String errorMessage;
    
    private String notifyStatus;
    private String notifyStatusName;
    private String notifyResult;
    
    private LocalDateTime createdAt;
}
