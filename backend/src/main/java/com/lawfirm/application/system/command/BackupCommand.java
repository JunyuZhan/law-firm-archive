package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 备份命令
 */
@Data
public class BackupCommand {
    @NotBlank(message = "备份类型不能为空")
    private String backupType; // FULL, INCREMENTAL, DATABASE, FILE

    private String description; // 备份说明
}

