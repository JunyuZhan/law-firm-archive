package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.entity.OperationLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口.
 * @author junyuzhan
 */
public interface OperationLogService {

    /**
     * 记录操作日志.
     */
    void log(OperationLog log);

    /**
     * 记录操作日志（简化版）.
     */
    void log(String objectType, String objectId, Long archiveId, String operationType, String desc);

    /**
     * 记录操作日志（带详情）.
     */
    void log(String objectType, String objectId, Long archiveId, String operationType, String desc, Map<String, Object> detail);

    /**
     * 分页查询日志.
     */
    PageResult<OperationLog> query(String keyword, String objectType, String operationType, 
            Long operatorId, LocalDate startDate, LocalDate endDate, Integer pageNum, Integer pageSize);

    /**
     * 根据档案ID查询操作日志.
     */
    List<OperationLog> getByArchiveId(Long archiveId);

    /**
     * 根据对象查询操作日志.
     */
    List<OperationLog> getByObject(String objectType, String objectId);

    /**
     * 获取操作统计.
     */
    Map<String, Long> getOperationStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 导出日志.
     */
    byte[] exportLogs(String objectType, String operationType, Long operatorId, LocalDate startDate, LocalDate endDate);
}
