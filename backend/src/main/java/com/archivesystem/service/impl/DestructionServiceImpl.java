package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.DestructionRecord;
import com.archivesystem.entity.OperationLog;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.DestructionRecordMapper;
import com.archivesystem.repository.OperationLogMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.ArchiveService;
import com.archivesystem.service.DestructionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 销毁管理服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionServiceImpl implements DestructionService {

    private final DestructionRecordMapper destructionMapper;
    private final ArchiveMapper archiveMapper;
    private final OperationLogMapper operationLogMapper;
    private final ArchiveService archiveService;

    private static final AtomicInteger batchNoCounter = new AtomicInteger(1);

    @Override
    @Transactional
    public DestructionRecord apply(Long archiveId, String destructionReason, String destructionMethod) {
        // 检查档案是否存在
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveId);
        }

        // 检查档案状态
        if (Archive.STATUS_DESTROYED.equals(archive.getStatus())) {
            throw new BusinessException("该档案已销毁");
        }
        if (Archive.STATUS_BORROWED.equals(archive.getStatus())) {
            throw new BusinessException("该档案正在借阅中，无法申请销毁");
        }

        // 检查是否已有未完成的销毁申请
        List<DestructionRecord> existingRecords = destructionMapper.selectByArchiveId(archiveId);
        for (DestructionRecord record : existingRecords) {
            if (DestructionRecord.STATUS_PENDING.equals(record.getStatus()) 
                || DestructionRecord.STATUS_APPROVED.equals(record.getStatus())) {
                throw new BusinessException("该档案已有未完成的销毁申请");
            }
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentRealName();

        DestructionRecord record = DestructionRecord.builder()
                .destructionBatchNo(generateBatchNo())
                .archiveId(archiveId)
                .destructionReason(destructionReason)
                .destructionMethod(destructionMethod != null ? destructionMethod : DestructionRecord.METHOD_LOGICAL)
                .status(DestructionRecord.STATUS_PENDING)
                .proposerId(userId)
                .proposerName(userName)
                .proposedAt(LocalDateTime.now())
                .build();

        destructionMapper.insert(record);

        // 更新档案状态为鉴定中
        archive.setStatus(Archive.STATUS_APPRAISAL);
        archiveMapper.updateById(archive);

        // 记录操作日志
        recordOperationLog(archiveId, "APPLY_DESTRUCTION", "申请档案销毁", userId, userName);

        log.info("申请档案销毁: id={}, archiveId={}, proposer={}", record.getId(), archiveId, userName);
        return record;
    }

    @Override
    @Transactional
    public List<DestructionRecord> batchApply(List<Long> archiveIds, String destructionReason, String destructionMethod) {
        List<DestructionRecord> records = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        String batchNo = generateBatchNo();

        for (Long archiveId : archiveIds) {
            try {
                Archive archive = archiveMapper.selectById(archiveId);
                if (archive == null || Archive.STATUS_DESTROYED.equals(archive.getStatus())) {
                    continue;
                }

                Long userId = SecurityUtils.getCurrentUserId();
                String userName = SecurityUtils.getCurrentRealName();

                DestructionRecord record = DestructionRecord.builder()
                        .destructionBatchNo(batchNo)
                        .archiveId(archiveId)
                        .destructionReason(destructionReason)
                        .destructionMethod(destructionMethod != null ? destructionMethod : DestructionRecord.METHOD_LOGICAL)
                        .status(DestructionRecord.STATUS_PENDING)
                        .proposerId(userId)
                        .proposerName(userName)
                        .proposedAt(LocalDateTime.now())
                        .build();

                destructionMapper.insert(record);
                records.add(record);

                archive.setStatus(Archive.STATUS_APPRAISAL);
                archiveMapper.updateById(archive);
            } catch (Exception e) {
                log.error("批量申请销毁失败: archiveId={}", archiveId, e);
                failedIds.add(archiveId);
            }
        }

        // 如果有失败的记录，抛出异常回滚事务
        if (!failedIds.isEmpty()) {
            throw new BusinessException(String.format("部分档案申请销毁失败，已回滚。失败的档案ID: %s", failedIds));
        }

        log.info("批量申请销毁: batchNo={}, count={}", batchNo, records.size());
        return records;
    }

    @Override
    public DestructionRecord getById(Long id) {
        DestructionRecord record = destructionMapper.selectById(id);
        if (record == null) {
            throw NotFoundException.of("销毁记录", id);
        }
        assertArchiveReadableForDestruction(record.getArchiveId());
        return record;
    }

    @Override
    public PageResult<DestructionRecord> getList(String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<DestructionRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(DestructionRecord::getStatus, status);
        }
        
        wrapper.orderByDesc(DestructionRecord::getCreatedAt);

        Page<DestructionRecord> page = new Page<>(pageNum, pageSize);
        Page<DestructionRecord> result = destructionMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<DestructionRecord> getPendingList(Integer pageNum, Integer pageSize) {
        Page<DestructionRecord> page = new Page<>(pageNum, pageSize);
        Page<DestructionRecord> result = destructionMapper.selectPage(page,
                new LambdaQueryWrapper<DestructionRecord>()
                        .eq(DestructionRecord::getStatus, DestructionRecord.STATUS_PENDING)
                        .orderByAsc(DestructionRecord::getCreatedAt));

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<DestructionRecord> getApprovedList(Integer pageNum, Integer pageSize) {
        Page<DestructionRecord> page = new Page<>(pageNum, pageSize);
        Page<DestructionRecord> result = destructionMapper.selectPage(page,
                new LambdaQueryWrapper<DestructionRecord>()
                        .eq(DestructionRecord::getStatus, DestructionRecord.STATUS_APPROVED)
                        .orderByAsc(DestructionRecord::getApprovedAt));

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public List<DestructionRecord> getByArchiveId(Long archiveId) {
        assertArchiveReadableForDestruction(archiveId);
        return destructionMapper.selectByArchiveId(archiveId);
    }

    private void assertArchiveReadableForDestruction(Long archiveId) {
        if (archiveId == null) {
            throw new BusinessException("档案标识无效");
        }
        archiveService.getById(archiveId);
    }

    @Override
    @Transactional
    public void approve(Long id, String comment) {
        // 检查记录是否存在
        DestructionRecord record = getById(id);
        
        // 使用条件更新防止并发重复审批
        LambdaUpdateWrapper<DestructionRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DestructionRecord::getId, id)
                .eq(DestructionRecord::getStatus, DestructionRecord.STATUS_PENDING)
                .set(DestructionRecord::getStatus, DestructionRecord.STATUS_APPROVED)
                .set(DestructionRecord::getApproverId, SecurityUtils.getCurrentUserId())
                .set(DestructionRecord::getApproverName, SecurityUtils.getCurrentRealName())
                .set(DestructionRecord::getApprovedAt, LocalDateTime.now())
                .set(DestructionRecord::getApprovalComment, comment);
        
        int affected = destructionMapper.update(null, updateWrapper);
        if (affected == 0) {
            DestructionRecord current = destructionMapper.selectById(id);
            if (current == null) {
                throw new NotFoundException("销毁记录不存在");
            }
            throw new BusinessException("该记录不是待审批状态，当前状态：" + current.getStatus());
        }
        log.info("销毁申请审批通过: id={}, archiveId={}", id, record.getArchiveId());
    }

    @Override
    @Transactional
    public void reject(Long id, String comment) {
        // 检查记录是否存在
        DestructionRecord record = getById(id);
        
        // 使用条件更新防止并发重复审批
        LambdaUpdateWrapper<DestructionRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DestructionRecord::getId, id)
                .eq(DestructionRecord::getStatus, DestructionRecord.STATUS_PENDING)
                .set(DestructionRecord::getStatus, DestructionRecord.STATUS_REJECTED)
                .set(DestructionRecord::getApproverId, SecurityUtils.getCurrentUserId())
                .set(DestructionRecord::getApproverName, SecurityUtils.getCurrentRealName())
                .set(DestructionRecord::getApprovedAt, LocalDateTime.now())
                .set(DestructionRecord::getApprovalComment, comment);
        
        int affected = destructionMapper.update(null, updateWrapper);
        if (affected == 0) {
            DestructionRecord current = destructionMapper.selectById(id);
            if (current == null) {
                throw new NotFoundException("销毁记录不存在");
            }
            throw new BusinessException("该记录不是待审批状态，当前状态：" + current.getStatus());
        }

        // 使用条件更新恢复档案状态（只有当档案状态为 APPRAISAL 时才更新）
        LambdaUpdateWrapper<Archive> archiveUpdateWrapper = new LambdaUpdateWrapper<>();
        archiveUpdateWrapper.eq(Archive::getId, record.getArchiveId())
                .eq(Archive::getStatus, Archive.STATUS_APPRAISAL)
                .set(Archive::getStatus, Archive.STATUS_STORED);
        archiveMapper.update(null, archiveUpdateWrapper);

        log.info("销毁申请被拒绝: id={}", id);
    }

    @Override
    @Transactional
    public void execute(Long id, String remarks) {
        DestructionRecord record = getById(id);

        if (!DestructionRecord.STATUS_APPROVED.equals(record.getStatus())) {
            throw new BusinessException("该记录未审批通过，无法执行销毁");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentRealName();

        record.setStatus(DestructionRecord.STATUS_EXECUTED);
        record.setExecutorId(userId);
        record.setExecutorName(userName);
        record.setExecutedAt(LocalDateTime.now());

        destructionMapper.updateById(record);

        // 更新档案状态
        Archive archive = archiveMapper.selectById(record.getArchiveId());
        if (archive != null) {
            archive.setStatus(Archive.STATUS_DESTROYED);
            archiveMapper.updateById(archive);

            // 记录操作日志
            recordOperationLog(archive.getId(), "EXECUTE_DESTRUCTION", "执行档案销毁", userId, userName);
        }

        log.info("档案销毁执行: id={}, archiveId={}, executor={}", id, record.getArchiveId(), userName);
    }

    @Override
    @Transactional
    public void batchExecute(List<Long> ids, String remarks) {
        for (Long id : ids) {
            try {
                execute(id, remarks);
            } catch (Exception e) {
                log.error("批量执行销毁失败: id={}", id, e);
            }
        }
    }

    private String generateBatchNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = batchNoCounter.getAndIncrement();
        return String.format("DEST-%s-%04d", date, seq);
    }

    private void recordOperationLog(Long archiveId, String operationType, String description, Long userId, String userName) {
        OperationLog log = OperationLog.builder()
                .archiveId(archiveId)
                .objectType("ARCHIVE")
                .operationType(operationType)
                .operationDesc(description)
                .operatorId(userId)
                .operatorName(userName)
                .operatedAt(LocalDateTime.now())
                .build();
        operationLogMapper.insert(log);
    }
}
