package com.lawfirm.application.system.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 数据库迁移DTO. */
@Data
public class MigrationDTO {
  /** ID. */
  private Long id;

  /** 迁移编号. */
  private String migrationNo;

  /** 版本号. */
  private String version;

  /** 脚本名称. */
  private String scriptName;

  /** 脚本路径. */
  private String scriptPath;

  /** 描述. */
  private String description;

  /** 状态. */
  private String status;

  /** 执行时间. */
  private LocalDateTime executedAt;

  /** 执行耗时(毫秒). */
  private Long executionTimeMs;

  /** 错误信息. */
  private String errorMessage;

  /** 执行人ID. */
  private Long executedBy;

  /** 执行人姓名. */
  private String executedByName;

  /** 创建时间. */
  private LocalDateTime createdAt;

  /** 更新时间. */
  private LocalDateTime updatedAt;
}
