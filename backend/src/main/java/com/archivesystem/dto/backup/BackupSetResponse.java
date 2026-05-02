package com.archivesystem.dto.backup;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private String backupNo;
    private String backupSetName;
    @JsonIgnore
    private String backupSetPath;
    private String displayPath;
    private LocalDateTime createdAt;
    private String databaseMode;
    private Boolean databaseRestorable;
    private Boolean filesRestorable;
    private Boolean configRestorable;
    private Long fileCount;
    private Long objectCount;
    private Long totalBytes;
    @JsonIgnore
    private String filesIndex;
    @JsonIgnore
    private Boolean hasManifest;
    @JsonIgnore
    private Boolean hasChecksums;
    private String verifyStatus;
    private String verifyMessage;
}
