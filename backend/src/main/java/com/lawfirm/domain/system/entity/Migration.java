package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * 数据库迁移记录实体
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_migration")
public class Migration extends BaseEntity {

    /** 迁移编号 */
    private String migrationNo;

    /** 迁移版本号，如 V1.0.1（使用schemaVersion避免与BaseEntity.version乐观锁字段冲突） */
    @TableField("version")
    private String schemaVersion;

    /** 脚本文件名 */
    private String scriptName;

    /** 脚本文件路径 */
    private String scriptPath;

    /** 描述 */
    private String description;

    /** 状态：PENDING-待执行, SUCCESS-成功, FAILED-失败, ROLLED_BACK-已回滚 */
    private String status;

    /** 执行时间 */
    private LocalDateTime executedAt;

    /** 执行耗时（毫秒） */
    private Long executionTimeMs;

    /** 错误信息 */
    private String errorMessage;

    /** 执行人ID */
    private Long executedBy;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";
}

