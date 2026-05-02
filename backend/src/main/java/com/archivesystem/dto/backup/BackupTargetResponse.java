package com.archivesystem.dto.backup;

import lombok.Builder;
import lombok.Data;

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
    private String displayAddress;
    private String localPath;
    private String smbHost;
    private Integer smbPort;
    private String smbShare;
    private String smbUsername;
    private String smbSubPath;
    private String remarks;
    private String verifyStatus;
    private String verifyMessage;
}
