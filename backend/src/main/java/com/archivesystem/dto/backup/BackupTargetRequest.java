package com.archivesystem.dto.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 备份目标请求.
 * @author junyuzhan
 */
@Data
public class BackupTargetRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String targetType;

    @NotNull
    private Boolean enabled;

    private String localPath;
    private String smbHost;
    private Integer smbPort;
    private String smbShare;
    private String smbUsername;
    private String smbPassword;
    private String smbSubPath;
    private String remarks;
}
