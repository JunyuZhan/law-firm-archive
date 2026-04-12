package com.archivesystem.service;

import com.archivesystem.entity.PushRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Map;

/**
 * 推送记录服务接口.
 * @author junyuzhan
 */
public interface PushRecordService {

    /**
     * 创建推送记录
     */
    PushRecord create(PushRecord record);

    /**
     * 根据ID查询
     */
    PushRecord getById(Long id);

    /**
     * 根据来源类型和ID查询
     */
    PushRecord getBySourceTypeAndId(String sourceType, String sourceId);

    /**
     * 分页查询
     */
    IPage<PushRecord> page(Page<PushRecord> page, String sourceType, String pushStatus, String keyword, String pushBatchNo, LocalDateTime pushedAtStart, LocalDateTime pushedAtEnd);

    /**
     * 更新状态
     */
    void updateStatus(Long id, String status, String errorMessage);

    /**
     * 更新处理结果
     */
    void updateResult(Long id, Long archiveId, String archiveNo, String status, 
                      int successFiles, int failedFiles);

    /**
     * 获取统计数据
     */
    Map<String, Object> getStatistics();

    /**
     * 获取待处理的推送记录
     */
    List<PushRecord> getPendingRecords();

    /**
     * 获取失败的推送记录
     */
    List<PushRecord> getFailedRecords();

    /**
     * 重试推送
     */
    void retry(Long id);
}
