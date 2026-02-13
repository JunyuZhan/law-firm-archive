package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** 数据库迁移记录实体. */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_migration")
public class Migration extends BaseEntity {

  /** 迁移编号. */
  private String migrationNo;

  /** 迁移版本号，如 V1.0.1（使用schemaVersion避免与BaseEntity.version乐观锁字段冲突）. */
  @TableField("version")
  private String schemaVersion;

  /** 脚本文件名. */
  private String scriptName;

  /** 脚本文件路径. */
  private String scriptPath;

  /** 描述. */
  private String description;

  /** 状态：PENDING-待执行, SUCCESS-成功, FAILED-失败, ROLLED_BACK-已回滚. */
  private String status;

  /** 执行时间. */
  private LocalDateTime executedAt;

  /** 执行耗时（毫秒）. */
  private Long executionTimeMs;

  /** 错误信息. */
  private String errorMessage;

  /** 执行人ID. */
  private Long executedBy;

  /** 状态：待执行. */
  public static final String STATUS_PENDING = "PENDING";

  /** 状态：成功. */
  public static final String STATUS_SUCCESS = "SUCCESS";

  /** 状态：失败. */
  public static final String STATUS_FAILED = "FAILED";

  /** 状态：已回滚. */
  public static final String STATUS_ROLLED_BACK = "ROLLED_BACK";
}
