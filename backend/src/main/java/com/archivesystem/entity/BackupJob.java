package com.archivesystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备份任务记录.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_backup_job")
public class BackupJob implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String backupNo;
    private Long targetId;
    private String targetName;
    private String triggerType;
    private String backupScope;
    private String status;
    @JsonIgnore
    private String backupSetPath;
    private Long fileCount;
    private Long totalBytes;
    @JsonIgnore
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
