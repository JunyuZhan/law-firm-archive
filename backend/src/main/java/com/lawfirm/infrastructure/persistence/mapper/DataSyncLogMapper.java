package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.system.entity.DataSyncLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据同步日志 Mapper
 * 
 * Requirements: 1.5
 */
@Mapper
public interface DataSyncLogMapper extends BaseMapper<DataSyncLog> {

    /**
     * 查询源记录的同步日志
     */
    @Select("SELECT * FROM sys_data_sync_log WHERE source_table = #{sourceTable} AND source_id = #{sourceId} ORDER BY synced_at DESC")
    List<DataSyncLog> selectBySource(@Param("sourceTable") String sourceTable, @Param("sourceId") Long sourceId);

    /**
     * 查询失败的同步日志（用于重试）
     */
    @Select("SELECT * FROM sys_data_sync_log WHERE sync_status = 'FAILED' AND retry_count < #{maxRetry} ORDER BY synced_at ASC")
    List<DataSyncLog> selectFailedLogs(@Param("maxRetry") int maxRetry);

    /**
     * 查询目标模块的同步日志
     */
    @Select("SELECT * FROM sys_data_sync_log WHERE target_module = #{targetModule} ORDER BY synced_at DESC LIMIT #{limit}")
    List<DataSyncLog> selectByTargetModule(@Param("targetModule") String targetModule, @Param("limit") int limit);
}
