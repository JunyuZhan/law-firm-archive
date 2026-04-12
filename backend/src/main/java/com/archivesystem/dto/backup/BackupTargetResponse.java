package com.archivesystem.dto.backup;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 备份目标响应.
 * @author junyuzhan
 */
@Data
@Builder
public class BackupTargetResponse {
    private Long id;
    private String name;
    private String targetType;
    private Boolean enabled;
    private String localPath;
    private String smbHost;
    private Integer smbPort;
    private String smbShare;
    private String smbUsername;
    private Boolean hasSmbPassword;
    private String smbSubPath;
    private String remarks;
    private String verifyStatus;
    private String verifyMessage;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
