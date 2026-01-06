package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.ExportLog;
import com.lawfirm.infrastructure.persistence.mapper.ExportLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 导出日志仓储
 * 
 * Requirements: 6.7
 */
@Repository
public class ExportLogRepository extends AbstractRepository<ExportLogMapper, ExportLog> {

    /**
     * 查询用户的导出日志
     */
    public List<ExportLog> findByExportedBy(Long userId, int limit) {
        return baseMapper.selectByExportedBy(userId, limit);
    }

    /**
     * 查询指定类型的导出日志
     */
    public List<ExportLog> findByExportType(String exportType, int limit) {
        return baseMapper.selectByExportType(exportType, limit);
    }
}
