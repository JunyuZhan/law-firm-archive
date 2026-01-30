package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 备份查询DTO. */
@Data
@EqualsAndHashCode(callSuper = true)
public class BackupQueryDTO extends PageQuery {
  /** 备份类型. */
  private String backupType;

  /** 状态. */
  private String status;

  /** 开始时间. */
  private LocalDateTime startTime;

  /** 结束时间. */
  private LocalDateTime endTime;
}
