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
 * 恢复任务记录.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("arc_restore_job")
public class RestoreJob implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String restoreNo;
    private String sourceType;
    private Long targetId;
    private String targetName;
    private String backupSetName;
    private String status;
    private String verifyStatus;
    private Boolean restoredDatabase;
    private Boolean restoredFiles;
    private Boolean restoredConfig;
    private String rebuildIndexStatus;
    private String restoreReport;
    @JsonIgnore
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
