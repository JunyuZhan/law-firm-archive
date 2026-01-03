package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 恢复命令
 */
@Data
public class RestoreCommand {
    @NotNull(message = "备份ID不能为空")
    private Long backupId;

    private String description; // 恢复说明
}

