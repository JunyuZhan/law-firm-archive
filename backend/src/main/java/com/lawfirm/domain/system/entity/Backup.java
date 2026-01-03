package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 系统备份实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_backup")
public class Backup extends BaseEntity {

    /**
     * 备份编号
     */
    private String backupNo;

    /**
     * 备份类型：FULL-全量, INCREMENTAL-增量, DATABASE-数据库, FILE-文件
     */
    private String backupType;

    /**
     * 备份名称
     */
    private String backupName;

    /**
     * 备份文件路径
     */
    private String backupPath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 状态：PENDING-进行中, SUCCESS-成功, FAILED-失败
     */
    private String status;

    /**
     * 备份时间
     */
    private LocalDateTime backupTime;

    /**
     * 恢复时间
     */
    private LocalDateTime restoreTime;

    /**
     * 备份说明
     */
    private String description;

    /**
     * 创建人ID
     */
    private Long createdBy;
}

