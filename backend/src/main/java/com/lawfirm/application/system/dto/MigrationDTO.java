package com.lawfirm.application.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据库迁移DTO
 */
@Data
public class MigrationDTO {
    private Long id;
    private String migrationNo;
    private String version;
    private String scriptName;
    private String scriptPath;
    private String description;
    private String status;
    private LocalDateTime executedAt;
    private Long executionTimeMs;
    private String errorMessage;
    private Long executedBy;
    private String executedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

