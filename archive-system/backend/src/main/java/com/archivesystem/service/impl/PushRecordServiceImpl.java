package com.archivesystem.service.impl;

import com.archivesystem.entity.PushRecord;
import com.archivesystem.repository.PushRecordMapper;
import com.archivesystem.service.PushRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 推送记录服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushRecordServiceImpl implements PushRecordService {

    private final PushRecordMapper pushRecordMapper;

    @Override
    @Transactional
    public PushRecord create(PushRecord record) {
        if (!StringUtils.hasText(record.getPushBatchNo())) {
            record.setPushBatchNo(generateBatchNo());
        }
        if (record.getPushStatus() == null) {
            record.setPushStatus(PushRecord.STATUS_PENDING);
        }
        if (record.getPushedAt() == null) {
            record.setPushedAt(LocalDateTime.now());
        }
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        pushRecordMapper.insert(record);
        return record;
    }

    @Override
    public PushRecord getById(Long id) {
        return pushRecordMapper.selectById(id);
    }

    @Override
    public PushRecord getBySourceTypeAndId(String sourceType, String sourceId) {
        return pushRecordMapper.selectBySourceTypeAndId(sourceType, sourceId);
    }

    @Override
    public IPage<PushRecord> page(Page<PushRecord> page, String sourceType, String pushStatus, String keyword) {
        LambdaQueryWrapper<PushRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecord::getDeleted, false);
        
        if (StringUtils.hasText(sourceType)) {
            wrapper.eq(PushRecord::getSourceType, sourceType);
        }
        if (StringUtils.hasText(pushStatus)) {
            wrapper.eq(PushRecord::getPushStatus, pushStatus);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                .like(PushRecord::getTitle, keyword)
                .or().like(PushRecord::getSourceId, keyword)
                .or().like(PushRecord::getSourceNo, keyword)
                .or().like(PushRecord::getArchiveNo, keyword)
            );
        }
        
        wrapper.orderByDesc(PushRecord::getPushedAt);
        return pushRecordMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status, String errorMessage) {
        PushRecord record = pushRecordMapper.selectById(id);
        if (record != null) {
            record.setPushStatus(status);
            record.setErrorMessage(errorMessage);
            record.setUpdatedAt(LocalDateTime.now());
            if (PushRecord.STATUS_SUCCESS.equals(status) || PushRecord.STATUS_FAILED.equals(status)) {
                record.setProcessedAt(LocalDateTime.now());
            }
            pushRecordMapper.updateById(record);
        }
    }

    @Override
    @Transactional
    public void updateResult(Long id, Long archiveId, String archiveNo, String status,
                             int successFiles, int failedFiles) {
        PushRecord record = pushRecordMapper.selectById(id);
        if (record != null) {
            record.setArchiveId(archiveId);
            record.setArchiveNo(archiveNo);
            record.setPushStatus(status);
            record.setSuccessFiles(successFiles);
            record.setFailedFiles(failedFiles);
            record.setProcessedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            pushRecordMapper.updateById(record);
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 统计各状态数量
        List<Map<String, Object>> statusCounts = pushRecordMapper.countByStatus();
        Map<String, Long> statusMap = new HashMap<>();
        long total = 0;
        for (Map<String, Object> item : statusCounts) {
            String status = (String) item.get("push_status");
            Long count = ((Number) item.get("count")).longValue();
            statusMap.put(status, count);
            total += count;
        }
        stats.put("total", total);
        stats.put("pending", statusMap.getOrDefault(PushRecord.STATUS_PENDING, 0L));
        stats.put("processing", statusMap.getOrDefault(PushRecord.STATUS_PROCESSING, 0L));
        stats.put("success", statusMap.getOrDefault(PushRecord.STATUS_SUCCESS, 0L));
        stats.put("failed", statusMap.getOrDefault(PushRecord.STATUS_FAILED, 0L));
        stats.put("partial", statusMap.getOrDefault(PushRecord.STATUS_PARTIAL, 0L));
        
        // 今日推送数
        stats.put("today", pushRecordMapper.countToday());
        
        return stats;
    }

    @Override
    public List<PushRecord> getPendingRecords() {
        return pushRecordMapper.selectPendingRecords();
    }

    @Override
    public List<PushRecord> getFailedRecords() {
        return pushRecordMapper.selectFailedRecords();
    }

    @Override
    @Transactional
    public void retry(Long id) {
        PushRecord record = pushRecordMapper.selectById(id);
        if (record != null && PushRecord.STATUS_FAILED.equals(record.getPushStatus())) {
            record.setPushStatus(PushRecord.STATUS_PENDING);
            record.setErrorMessage(null);
            record.setUpdatedAt(LocalDateTime.now());
            pushRecordMapper.updateById(record);
            log.info("推送记录已重置为待处理: id={}", id);
        }
    }

    private String generateBatchNo() {
        return "PUSH-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
