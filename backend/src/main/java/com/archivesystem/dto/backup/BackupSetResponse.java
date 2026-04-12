package com.archivesystem.dto.backup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 备份集摘要信息.
 * @author junyuzhan
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupSetResponse {

    private Long targetId;
    private String targetName;
    private String targetType;
    private String backupNo;
    private String backupSetName;
    private String backupSetPath;
    private LocalDateTime createdAt;
    private String databaseMode;
    private Long fileCount;
    private Long objectCount;
    private Long totalBytes;
    private String filesIndex;
    private Boolean hasManifest;
    private Boolean hasChecksums;
    private String verifyStatus;
    private String verifyMessage;
}
