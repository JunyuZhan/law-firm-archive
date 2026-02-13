package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 备份DTO. */
@Data
public class BackupDTO {
  /** ID. */
  private Long id;

  /** 备份编号. */
  private String backupNo;

  /** 备份类型. */
  private String backupType;

  /** 备份名称. */
  private String backupName;

  /** 备份路径. */
  private String backupPath;

  /** 文件大小. */
  private Long fileSize;

  /** 状态. */
  private String status;

  /** 备份时间. */
  private LocalDateTime backupTime;

  /** 恢复时间. */
  private LocalDateTime restoreTime;

  /** 描述. */
  private String description;

  /** 创建人ID. */
  private Long createdBy;

  /** 创建人姓名. */
  private String createdByName;

  /** 创建时间. */
  private LocalDateTime createdAt;
}
