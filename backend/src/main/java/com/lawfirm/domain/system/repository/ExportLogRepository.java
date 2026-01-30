package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.ExportLog;
import com.lawfirm.infrastructure.persistence.mapper.ExportLogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 导出日志仓储。
 *
 * <p>提供导出日志的持久化操作。
 *
 * <p>Requirements: 6.7
 */
@Repository
public class ExportLogRepository extends AbstractRepository<ExportLogMapper, ExportLog> {

  /**
   * 查询用户的导出日志。
   *
   * @param userId 用户ID
   * @param limit 查询数量限制
   * @return 导出日志列表
   */
  public List<ExportLog> findByExportedBy(final Long userId, final int limit) {
    return baseMapper.selectByExportedBy(userId, limit);
  }

  /**
   * 查询指定类型的导出日志。
   *
   * @param exportType 导出类型
   * @param limit 查询数量限制
   * @return 导出日志列表
   */
  public List<ExportLog> findByExportType(final String exportType, final int limit) {
    return baseMapper.selectByExportType(exportType, limit);
  }
}
