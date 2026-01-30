package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.DataSyncLog;
import com.lawfirm.infrastructure.persistence.mapper.DataSyncLogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 数据同步日志仓储。
 *
 * <p>提供数据同步日志的持久化操作。
 *
 * <p>Requirements: 1.5
 */
@Repository
public class DataSyncLogRepository extends AbstractRepository<DataSyncLogMapper, DataSyncLog> {

  /**
   * 查询源记录的同步日志。
   *
   * @param sourceTable 源表名
   * @param sourceId 源记录ID
   * @return 同步日志列表
   */
  public List<DataSyncLog> findBySource(final String sourceTable, final Long sourceId) {
    return baseMapper.selectBySource(sourceTable, sourceId);
  }

  /**
   * 查询失败的同步日志（用于重试）。
   *
   * @param maxRetry 最大重试次数
   * @return 失败的同步日志列表
   */
  public List<DataSyncLog> findFailedLogs(final int maxRetry) {
    return baseMapper.selectFailedLogs(maxRetry);
  }

  /**
   * 查询目标模块的同步日志。
   *
   * @param targetModule 目标模块
   * @param limit 查询数量限制
   * @return 同步日志列表
   */
  public List<DataSyncLog> findByTargetModule(final String targetModule, final int limit) {
    return baseMapper.selectByTargetModule(targetModule, limit);
  }
}
