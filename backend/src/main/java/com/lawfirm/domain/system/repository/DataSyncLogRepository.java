package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.DataSyncLog;
import com.lawfirm.infrastructure.persistence.mapper.DataSyncLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据同步日志仓储
 * 
 * Requirements: 1.5
 */
@Repository
public class DataSyncLogRepository extends AbstractRepository<DataSyncLogMapper, DataSyncLog> {

    /**
     * 查询源记录的同步日志
     */
    public List<DataSyncLog> findBySource(String sourceTable, Long sourceId) {
        return baseMapper.selectBySource(sourceTable, sourceId);
    }

    /**
     * 查询失败的同步日志（用于重试）
     */
    public List<DataSyncLog> findFailedLogs(int maxRetry) {
        return baseMapper.selectFailedLogs(maxRetry);
    }

    /**
     * 查询目标模块的同步日志
     */
    public List<DataSyncLog> findByTargetModule(String targetModule, int limit) {
        return baseMapper.selectByTargetModule(targetModule, limit);
    }
}
