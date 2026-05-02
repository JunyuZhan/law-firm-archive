package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.common.util.ClientIpUtils;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DestructionRecord;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.entity.RetentionPeriod;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.DestructionRecordMapper;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.repository.RetentionPeriodMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.RetentionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 保管期限服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetentionServiceImpl implements RetentionService {

    private final ArchiveMapper archiveMapper;
    private final RetentionPeriodMapper retentionPeriodMapper;
    private final OperationLogMapper operationLogMapper;
    private final DestructionRecordMapper destructionRecordMapper;

    @Override
    public List<Archive> findExpiringArchives(int daysBeforeExpiry) {
        LocalDate expiryDate = LocalDate.now().plusDays(daysBeforeExpiry);
        
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getDeleted, false)
               .ne(Archive::getStatus, Archive.STATUS_DESTROYED)
               .isNotNull(Archive::getRetentionExpireDate)
               .le(Archive::getRetentionExpireDate, expiryDate)
               .ge(Archive::getRetentionExpireDate, LocalDate.now())
               .orderByAsc(Archive::getRetentionExpireDate);
        
        return archiveMapper.selectList(wrapper);
    }

    @Override
    public List<Archive> findExpiredArchives() {
        LocalDate today = LocalDate.now();
        
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getDeleted, false)
               .ne(Archive::getStatus, Archive.STATUS_DESTROYED)
               .isNotNull(Archive::getRetentionExpireDate)
               .lt(Archive::getRetentionExpireDate, today)
               .orderByAsc(Archive::getRetentionExpireDate);
        
        return archiveMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void extendRetention(Long archiveId, String newRetentionPeriod, String reason) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveId);
        }

        RetentionPeriod retention = retentionPeriodMapper.selectByPeriodCode(newRetentionPeriod);
        if (retention == null) {
            throw new BusinessException("无效的保管期限代码: " + newRetentionPeriod);
        }

        String oldRetentionPeriod = archive.getRetentionPeriod();
        LocalDate oldExpireDate = archive.getRetentionExpireDate();

        // 更新保管期限
        archive.setRetentionPeriod(newRetentionPeriod);
        
        // 计算新的到期日期
        LocalDate baseDate = archive.getArchiveDate() != null ? archive.getArchiveDate() : LocalDate.now();
        if (retention.getPeriodYears() != null) {
            archive.setRetentionExpireDate(baseDate.plusYears(retention.getPeriodYears()));
        } else {
            archive.setRetentionExpireDate(null); // 永久保存
        }

        archiveMapper.updateById(archive);

        // 记录操作日志
        OperationLog operationLog = OperationLog.builder()
                .archiveId(archiveId)
                .objectType("ARCHIVE")
                .operationType("EXTEND_RETENTION")
                .operatorId(SecurityUtils.getCurrentUserId())
                .operatorName(SecurityUtils.getCurrentRealName())
                .operatorIp(getClientIp())
                .operationDetail(Map.of(
                        "oldRetentionPeriod", oldRetentionPeriod,
                        "newRetentionPeriod", newRetentionPeriod,
                        "oldExpireDate", oldExpireDate != null ? oldExpireDate.toString() : "",
                        "newExpireDate", archive.getRetentionExpireDate() != null ? 
                                archive.getRetentionExpireDate().toString() : "永久",
                        "reason", reason
                ))
                .build();
        operationLogMapper.insert(operationLog);

        log.info("延长档案保管期限: archiveId={}, {} -> {}", archiveId, oldRetentionPeriod, newRetentionPeriod);
    }

    @Override
    @Transactional
    public void applyForDestruction(Long archiveId, String reason) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveId);
        }

        if (Archive.STATUS_DESTROYED.equals(archive.getStatus())) {
            throw new BusinessException("档案已销毁");
        }

        // 更新状态为鉴定中
        archive.setStatus(Archive.STATUS_APPRAISAL);
        archiveMapper.updateById(archive);

        // 记录操作日志
        OperationLog operationLog = OperationLog.builder()
                .archiveId(archiveId)
                .objectType("ARCHIVE")
                .operationType("APPLY_DESTRUCTION")
                .operatorId(SecurityUtils.getCurrentUserId())
                .operatorName(SecurityUtils.getCurrentRealName())
                .operationDetail(Map.of("reason", reason))
                .build();
        operationLogMapper.insert(operationLog);

        log.info("申请档案销毁: archiveId={}", archiveId);
    }

    @Override
    @Transactional
    public void executeDestruction(Long archiveId, String remarks) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveId);
        }

        if (!Archive.STATUS_APPRAISAL.equals(archive.getStatus())) {
            throw new BusinessException("只有鉴定中的档案才能执行销毁");
        }

        // 【安全检查】必须有审批通过的销毁记录才能执行销毁
        LambdaQueryWrapper<DestructionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DestructionRecord::getArchiveId, archiveId)
               .eq(DestructionRecord::getStatus, DestructionRecord.STATUS_APPROVED);
        DestructionRecord approvedRecord = destructionRecordMapper.selectOne(wrapper);
        
        if (approvedRecord == null) {
            throw new BusinessException("档案销毁必须先通过审批流程（请通过销毁管理模块申请并获得审批）");
        }

        // 更新状态为已销毁
        archive.setStatus(Archive.STATUS_DESTROYED);
        archiveMapper.updateById(archive);
        
        // 更新销毁记录状态为已执行
        approvedRecord.setStatus(DestructionRecord.STATUS_EXECUTED);
        approvedRecord.setExecutedAt(LocalDateTime.now());
        approvedRecord.setExecutorId(SecurityUtils.getCurrentUserId());
        approvedRecord.setExecutorName(SecurityUtils.getCurrentRealName());
        destructionRecordMapper.updateById(approvedRecord);

        // 记录操作日志
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("approverId", approvedRecord.getApproverId());
        detail.put("remarks", remarks);
        detail.put("executedAt", LocalDateTime.now().toString());

        OperationLog operationLog = OperationLog.builder()
                .archiveId(archiveId)
                .objectType("ARCHIVE")
                .operationType("EXECUTE_DESTRUCTION")
                .operatorId(SecurityUtils.getCurrentUserId())
                .operatorName(SecurityUtils.getCurrentRealName())
                .operationDetail(detail)
                .build();
        operationLogMapper.insert(operationLog);

        log.info("执行档案销毁: archiveId={}", archiveId);
    }
    
    /**
     * 从当前请求中获取客户端IP
     */
    private String getClientIp() {
        try {
            return ClientIpUtils.resolveCurrentRequestIp();
        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
            return "";
        }
    }
}
