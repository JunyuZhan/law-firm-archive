package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 备份命令. */
@Data
public class BackupCommand {
  /** 备份类型 (FULL, INCREMENTAL, DATABASE, FILE). */
  @NotBlank(message = "备份类型不能为空")
  private String backupType;

  /** 备份说明. */
  private String description;
}
