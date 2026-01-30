package com.lawfirm.domain.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lawfirm.common.base.BaseEntity;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 数据同步日志实体 用于记录合同审批后数据同步到各模块的日志
 *
 * <p>Requirements: 1.5
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_sync_log")
public class DataSyncLog extends BaseEntity {

  /** 源表名 */
  private String sourceTable;

  /** 源记录ID */
  private Long sourceId;

  /** 目标模块：FINANCE-财务模块, ADMIN-行政模块 */
  private String targetModule;

  /** 操作类型：CREATE-创建, UPDATE-更新, DELETE-删除 */
  private String operationType;

  /** 同步的数据内容（JSON格式） */
  private String syncData;

  /** 同步状态：SUCCESS-成功, FAILED-失败, PENDING-待处理 */
  private String syncStatus;

  /** 错误信息 */
  private String errorMessage;

  /** 重试次数 */
  private Integer retryCount;

  /** 同步时间 */
  private LocalDateTime syncedAt;
}
