package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.AppraisalRecord;
import com.archivesystem.entity.Archive;
import com.archivesystem.repository.AppraisalRecordMapper;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.AppraisalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 鉴定管理服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppraisalServiceImpl implements AppraisalService {

    private final AppraisalRecordMapper appraisalMapper;
    private final ArchiveMapper archiveMapper;

    @Override
    @Transactional
    public AppraisalRecord create(Long archiveId, String appraisalType, String originalValue,
                                  String newValue, String appraisalReason) {
        // 检查档案是否存在
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveId);
        }

        // 验证鉴定类型
        if (!isValidType(appraisalType)) {
            throw new BusinessException("无效的鉴定类型: " + appraisalType);
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentRealName();

        AppraisalRecord record = AppraisalRecord.builder()
                .archiveId(archiveId)
                .appraisalType(appraisalType)
                .originalValue(originalValue)
                .newValue(newValue)
                .appraisalReason(appraisalReason)
                .status(AppraisalRecord.STATUS_PENDING)
                .appraiserId(userId)
                .appraiserName(userName)
                .appraisedAt(LocalDateTime.now())
                .build();

        appraisalMapper.insert(record);
        log.info("发起鉴定: id={}, archiveId={}, type={}", record.getId(), archiveId, appraisalType);

        return record;
    }

    @Override
    public AppraisalRecord getById(Long id) {
        AppraisalRecord record = appraisalMapper.selectById(id);
        if (record == null) {
            throw NotFoundException.of("鉴定记录", id);
        }
        return record;
    }

    @Override
    public PageResult<AppraisalRecord> getList(String type, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AppraisalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppraisalRecord::getDeleted, false);

        if (StringUtils.hasText(type)) {
            wrapper.eq(AppraisalRecord::getAppraisalType, type);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AppraisalRecord::getStatus, status);
        }

        wrapper.orderByDesc(AppraisalRecord::getCreatedAt);

        Page<AppraisalRecord> page = new Page<>(pageNum, pageSize);
        Page<AppraisalRecord> result = appraisalMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<AppraisalRecord> getPendingList(Integer pageNum, Integer pageSize) {
        Page<AppraisalRecord> page = new Page<>(pageNum, pageSize);
        Page<AppraisalRecord> result = appraisalMapper.selectPage(page,
                new LambdaQueryWrapper<AppraisalRecord>()
                        .eq(AppraisalRecord::getStatus, AppraisalRecord.STATUS_PENDING)
                        .eq(AppraisalRecord::getDeleted, false)
                        .orderByAsc(AppraisalRecord::getCreatedAt));

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public List<AppraisalRecord> getByArchiveId(Long archiveId) {
        return appraisalMapper.selectByArchiveId(archiveId);
    }

    @Override
    @Transactional
    public void approve(Long id, String comment) {
        AppraisalRecord record = getById(id);

        if (!AppraisalRecord.STATUS_PENDING.equals(record.getStatus())) {
            throw new BusinessException("该鉴定记录不是待审批状态");
        }

        record.setStatus(AppraisalRecord.STATUS_APPROVED);
        record.setApproverId(SecurityUtils.getCurrentUserId());
        record.setApproverName(SecurityUtils.getCurrentRealName());
        record.setApprovedAt(LocalDateTime.now());
        record.setApprovalComment(comment);

        appraisalMapper.updateById(record);

        // 根据鉴定类型更新档案
        applyAppraisalResult(record);

        log.info("鉴定审批通过: id={}, archiveId={}, type={}", id, record.getArchiveId(), record.getAppraisalType());
    }

    @Override
    @Transactional
    public void reject(Long id, String comment) {
        AppraisalRecord record = getById(id);

        if (!AppraisalRecord.STATUS_PENDING.equals(record.getStatus())) {
            throw new BusinessException("该鉴定记录不是待审批状态");
        }

        record.setStatus(AppraisalRecord.STATUS_REJECTED);
        record.setApproverId(SecurityUtils.getCurrentUserId());
        record.setApproverName(SecurityUtils.getCurrentRealName());
        record.setApprovedAt(LocalDateTime.now());
        record.setApprovalComment(comment);

        appraisalMapper.updateById(record);
        log.info("鉴定审批拒绝: id={}", id);
    }

    /**
     * 应用鉴定结果到档案.
     */
    private void applyAppraisalResult(AppraisalRecord record) {
        Archive archive = archiveMapper.selectById(record.getArchiveId());
        if (archive == null) {
            return;
        }

        String type = record.getAppraisalType();
        String newValue = record.getNewValue();

        switch (type) {
            case AppraisalRecord.TYPE_SECURITY:
                // 密级鉴定：更新密级
                archive.setSecurityLevel(newValue);
                break;
            case AppraisalRecord.TYPE_RETENTION:
                // 期限鉴定：更新保管期限
                archive.setRetentionPeriod(newValue);
                // 重新计算到期日期
                archive.setRetentionExpireDate(calculateExpireDate(newValue, archive.getArchiveDate()));
                break;
            case AppraisalRecord.TYPE_OPEN:
                // 开放鉴定：如果开放，则设置为公开
                if ("OPEN".equals(newValue) || "true".equalsIgnoreCase(newValue)) {
                    archive.setSecurityLevel(Archive.SECURITY_PUBLIC);
                }
                break;
            case AppraisalRecord.TYPE_VALUE:
                // 价值鉴定：仅记录，不修改档案
                break;
            default:
                log.warn("未知鉴定类型: {}", type);
        }

        archiveMapper.updateById(archive);
    }

    /**
     * 计算到期日期.
     */
    private LocalDate calculateExpireDate(String retentionPeriod, LocalDate baseDate) {
        if (baseDate == null) {
            baseDate = LocalDate.now();
        }
        
        switch (retentionPeriod) {
            case "PERMANENT":
                return LocalDate.of(9999, 12, 31);
            case "Y30":
                return baseDate.plusYears(30);
            case "Y15":
                return baseDate.plusYears(15);
            case "Y10":
                return baseDate.plusYears(10);
            case "Y5":
                return baseDate.plusYears(5);
            default:
                return baseDate.plusYears(10);
        }
    }

    private boolean isValidType(String type) {
        return AppraisalRecord.TYPE_VALUE.equals(type)
                || AppraisalRecord.TYPE_SECURITY.equals(type)
                || AppraisalRecord.TYPE_OPEN.equals(type)
                || AppraisalRecord.TYPE_RETENTION.equals(type);
    }
}
