package com.lawfirm.application.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备份DTO
 */
@Data
public class BackupDTO {
    private Long id;
    private String backupNo;
    private String backupType;
    private String backupName;
    private String backupPath;
    private Long fileSize;
    private String status;
    private LocalDateTime backupTime;
    private LocalDateTime restoreTime;
    private String description;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}

