package com.lawfirm.application.system.dto;

import com.lawfirm.common.base.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 备份查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BackupQueryDTO extends PageQuery {
    private String backupType;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

