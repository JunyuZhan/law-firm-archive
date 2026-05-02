package com.archivesystem.dto.backup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "LOCAL|SMB", message = "targetType 仅支持 LOCAL 或 SMB")
    private String targetType;

    @NotNull
    private Boolean enabled;

    private String localPath;
    private String smbHost;
    @Min(value = 1, message = "SMB 端口最小为1")
    @Max(value = 65535, message = "SMB 端口最大为65535")
    private Integer smbPort;
    private String smbShare;
    private String smbUsername;
    private String smbPassword;
    private String smbSubPath;
    private String remarks;
}
