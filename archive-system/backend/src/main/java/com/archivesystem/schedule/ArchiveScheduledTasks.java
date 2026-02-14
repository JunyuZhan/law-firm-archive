package com.archivesystem.schedule;

import com.archivesystem.entity.Archive;
import com.archivesystem.entity.PushRecord;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.PushRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 档案系统定时任务
 * 
 * 包含：
 * - PROCESSING 状态超时处理
 * - 过期档案提醒（待实现）
 * - 孤儿文件清理（待实现）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveScheduledTasks {

    private final ArchiveMapper archiveMapper;
    private final PushRecordMapper pushRecordMapper;

    // PROCESSING 状态超时时间（分钟），默认 30 分钟
    @Value("${archive.processing.timeout-minutes:30}")
    private int processingTimeoutMinutes;

    // 每次处理的最大档案数
    private static final int BATCH_SIZE = 100;

    /**
     * 处理超时的 PROCESSING 状态档案
     * 每 5 分钟执行一次
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void handleProcessingTimeout() {
        log.info("开始检查 PROCESSING 超时档案...");
        
        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(processingTimeoutMinutes);
            
            // 查询超时的 PROCESSING 状态档案
            LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Archive::getStatus, Archive.STATUS_PROCESSING)
                   .eq(Archive::getDeleted, false)
                   .lt(Archive::getUpdatedAt, timeoutThreshold)
                   .last("LIMIT " + BATCH_SIZE);
            
            List<Archive> timeoutArchives = archiveMapper.selectList(wrapper);
            
            if (timeoutArchives.isEmpty()) {
                log.debug("无超时的 PROCESSING 档案");
                return;
            }
            
            log.warn("发现 {} 个超时的 PROCESSING 档案", timeoutArchives.size());
            
            for (Archive archive : timeoutArchives) {
                handleSingleTimeoutArchive(archive);
            }
            
            log.info("PROCESSING 超时处理完成，处理数量: {}", timeoutArchives.size());
            
        } catch (Exception e) {
            log.error("处理 PROCESSING 超时档案异常", e);
        }
    }

    /**
     * 处理单个超时档案
     */
    private void handleSingleTimeoutArchive(Archive archive) {
        try {
            log.warn("处理超时档案: archiveId={}, archiveNo={}, updatedAt={}", 
                    archive.getId(), archive.getArchiveNo(), archive.getUpdatedAt());
            
            // 更新档案状态为失败
            String originalRemarks = archive.getRemarks();
            String timeoutRemark = String.format("[系统自动] PROCESSING 超时（超过 %d 分钟），标记为失败。时间: %s", 
                    processingTimeoutMinutes, LocalDateTime.now());
            
            archive.setStatus(Archive.STATUS_FAILED);
            archive.setRemarks(appendRemark(originalRemarks, timeoutRemark));
            archiveMapper.updateById(archive);
            
            // 更新关联的推送记录
            updateRelatedPushRecord(archive);
            
            // TODO: 发送告警通知
            sendTimeoutAlert(archive);
            
        } catch (Exception e) {
            log.error("处理超时档案失败: archiveId={}", archive.getId(), e);
        }
    }

    /**
     * 更新关联的推送记录
     */
    private void updateRelatedPushRecord(Archive archive) {
        if (archive.getSourceType() == null || archive.getSourceId() == null) {
            return;
        }
        
        LambdaQueryWrapper<PushRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecord::getSourceType, archive.getSourceType())
               .eq(PushRecord::getSourceId, archive.getSourceId())
               .eq(PushRecord::getPushStatus, PushRecord.STATUS_PROCESSING);
        
        PushRecord pushRecord = pushRecordMapper.selectOne(wrapper);
        if (pushRecord != null) {
            pushRecord.setPushStatus(PushRecord.STATUS_FAILED);
            pushRecord.setErrorMessage("PROCESSING 状态超时");
            pushRecord.setProcessedAt(LocalDateTime.now());
            pushRecordMapper.updateById(pushRecord);
            log.info("已更新推送记录状态: pushRecordId={}", pushRecord.getId());
        }
    }

    /**
     * 发送超时告警
     */
    private void sendTimeoutAlert(Archive archive) {
        // TODO: 接入告警服务（邮件、钉钉、短信等）
        log.error("【告警】档案处理超时！archiveId={}, archiveNo={}, sourceType={}, sourceId={}", 
                archive.getId(), archive.getArchiveNo(), archive.getSourceType(), archive.getSourceId());
    }

    /**
     * 追加备注
     */
    private String appendRemark(String existing, String newRemark) {
        if (existing == null || existing.isBlank()) {
            return newRemark;
        }
        return existing + "\n" + newRemark;
    }

    /**
     * 清理过期的 Redis 幂等标记
     * 每天凌晨 3 点执行（当前 Redis 已设置 TTL，此任务可选）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredIdempotentKeys() {
        log.info("Redis 幂等标记已设置 TTL，无需额外清理");
        // 当前实现中 Redis key 已设置 7 天 TTL，会自动过期
        // 如需更严格的清理，可在此添加扫描逻辑
    }

    /**
     * 过期档案提醒
     * 每天早上 8 点执行
     * 检查未来 30 天内即将到期的档案
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void remindExpiringArchives() {
        log.info("开始检查即将过期的档案...");
        
        try {
            LocalDate today = LocalDate.now();
            LocalDate expireThreshold = today.plusDays(30);
            
            // 查询未来 30 天内到期的档案（排除已删除的）
            LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Archive::getDeleted, false)
                   .eq(Archive::getStatus, Archive.STATUS_RECEIVED)
                   .isNotNull(Archive::getRetentionExpireDate)
                   .ge(Archive::getRetentionExpireDate, today)
                   .le(Archive::getRetentionExpireDate, expireThreshold)
                   .orderByAsc(Archive::getRetentionExpireDate);
            
            List<Archive> expiringArchives = archiveMapper.selectList(wrapper);
            
            if (expiringArchives.isEmpty()) {
                log.info("未发现即将过期的档案");
                return;
            }
            
            log.warn("发现 {} 个即将过期的档案（30天内）", expiringArchives.size());
            
            // 按到期时间分组统计
            Map<LocalDate, Long> expireByDate = expiringArchives.stream()
                    .collect(Collectors.groupingBy(Archive::getRetentionExpireDate, Collectors.counting()));
            
            // 区分 7 天内和 30 天内
            long within7Days = expiringArchives.stream()
                    .filter(a -> a.getRetentionExpireDate().isBefore(today.plusDays(8)))
                    .count();
            
            log.warn("其中 7 天内到期: {} 个", within7Days);
            
            // 记录详细信息
            for (Archive archive : expiringArchives) {
                long daysUntilExpire = java.time.temporal.ChronoUnit.DAYS.between(today, archive.getRetentionExpireDate());
                log.info("即将过期档案: archiveNo={}, title={}, expireDate={}, 剩余天数={}", 
                        archive.getArchiveNo(), archive.getTitle(), 
                        archive.getRetentionExpireDate(), daysUntilExpire);
            }
            
            // 发送提醒通知
            sendExpirationReminder(expiringArchives, within7Days);
            
        } catch (Exception e) {
            log.error("检查即将过期档案异常", e);
        }
    }
    
    /**
     * 发送过期提醒通知
     */
    private void sendExpirationReminder(List<Archive> expiringArchives, long urgentCount) {
        // TODO: 接入通知服务（邮件、钉钉、短信等）
        // 当前仅输出告警日志
        log.error("【告警】档案即将过期提醒！总计: {} 个，其中紧急（7天内）: {} 个", 
                expiringArchives.size(), urgentCount);
        
        // 可扩展：按档案负责人分组发送个性化通知
        // 可扩展：生成过期档案报告
    }
}
