package com.lawfirm.application.system.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 恢复命令. */
@Data
public class RestoreCommand {
  /** 备份ID */
  @NotNull(message = "备份ID不能为空")
  private Long backupId;

  /** 确认码（必须与 "RESTORE_" + 备份编号 匹配才能执行恢复） 用于防止误操作. */
  @NotNull(message = "确认码不能为空")
  private String confirmCode;

  /** 恢复说明 */
  private String description;
}
