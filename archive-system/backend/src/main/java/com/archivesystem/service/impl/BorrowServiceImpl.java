package com.archivesystem.service.impl;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Archive;
import com.archivesystem.entity.BorrowApplication;
import com.archivesystem.repository.ArchiveMapper;
import com.archivesystem.repository.BorrowApplicationMapper;
import com.archivesystem.security.SecurityUtils;
import com.archivesystem.service.BorrowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 借阅服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private final BorrowApplicationMapper borrowMapper;
    private final ArchiveMapper archiveMapper;

    private static final AtomicInteger applicationNoCounter = new AtomicInteger(1);

    @Override
    @Transactional
    public BorrowApplication apply(Long archiveId, String borrowPurpose, LocalDate expectedReturnDate, String remarks) {
        // 检查档案是否存在
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw NotFoundException.of("档案", archiveId);
        }

        // 检查档案是否已被借出
        if (Archive.STATUS_BORROWED.equals(archive.getStatus())) {
            throw new BusinessException("该档案已被借出，无法申请");
        }

        // 检查是否有未完成的借阅申请
        BorrowApplication existing = getCurrentByArchiveId(archiveId);
        if (existing != null) {
            throw new BusinessException("该档案已有未完成的借阅申请");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentRealName();

        BorrowApplication application = BorrowApplication.builder()
                .applicationNo(generateApplicationNo())
                .archiveId(archiveId)
                .archiveNo(archive.getArchiveNo())
                .archiveTitle(archive.getTitle())
                .applicantId(userId)
                .applicantName(userName)
                .borrowPurpose(borrowPurpose)
                .expectedReturnDate(expectedReturnDate)
                .remarks(remarks)
                .status(BorrowApplication.STATUS_PENDING)
                .applyTime(LocalDateTime.now())
                .build();

        borrowMapper.insert(application);
        log.info("借阅申请提交: id={}, archiveId={}, applicant={}", application.getId(), archiveId, userName);

        return application;
    }

    @Override
    public BorrowApplication getById(Long id) {
        BorrowApplication application = borrowMapper.selectById(id);
        if (application == null) {
            throw NotFoundException.of("借阅申请", id);
        }
        return application;
    }

    @Override
    public PageResult<BorrowApplication> getMyApplications(Long userId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getApplicantId, userId)
               .eq(BorrowApplication::getDeleted, false);

        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowApplication::getStatus, status);
        }

        wrapper.orderByDesc(BorrowApplication::getApplyTime);

        Page<BorrowApplication> page = new Page<>(pageNum, pageSize);
        Page<BorrowApplication> result = borrowMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        BorrowApplication application = getById(id);

        if (!BorrowApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("只能取消待审批的申请");
        }

        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!application.getApplicantId().equals(currentUserId)) {
            throw new BusinessException("只能取消自己的申请");
        }

        application.setStatus(BorrowApplication.STATUS_CANCELLED);
        borrowMapper.updateById(application);

        log.info("借阅申请取消: id={}", id);
    }

    @Override
    public PageResult<BorrowApplication> getPendingList(Integer pageNum, Integer pageSize) {
        Page<BorrowApplication> page = new Page<>(pageNum, pageSize);
        Page<BorrowApplication> result = borrowMapper.selectPage(page,
                new LambdaQueryWrapper<BorrowApplication>()
                        .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_PENDING)
                        .eq(BorrowApplication::getDeleted, false)
                        .orderByAsc(BorrowApplication::getApplyTime));

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<BorrowApplication> getApprovedList(Integer pageNum, Integer pageSize) {
        Page<BorrowApplication> page = new Page<>(pageNum, pageSize);
        Page<BorrowApplication> result = borrowMapper.selectPage(page,
                new LambdaQueryWrapper<BorrowApplication>()
                        .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_APPROVED)
                        .eq(BorrowApplication::getDeleted, false)
                        .orderByAsc(BorrowApplication::getApproveTime));

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional
    public void approve(Long id, String remarks) {
        BorrowApplication application = getById(id);

        if (!BorrowApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请不是待审批状态");
        }

        application.setStatus(BorrowApplication.STATUS_APPROVED);
        application.setApproverId(SecurityUtils.getCurrentUserId());
        application.setApproverName(SecurityUtils.getCurrentRealName());
        application.setApproveTime(LocalDateTime.now());
        application.setApproveRemarks(remarks);

        borrowMapper.updateById(application);
        log.info("借阅申请审批通过: id={}", id);
    }

    @Override
    @Transactional
    public void reject(Long id, String rejectReason) {
        BorrowApplication application = getById(id);

        if (!BorrowApplication.STATUS_PENDING.equals(application.getStatus())) {
            throw new BusinessException("该申请不是待审批状态");
        }

        application.setStatus(BorrowApplication.STATUS_REJECTED);
        application.setApproverId(SecurityUtils.getCurrentUserId());
        application.setApproverName(SecurityUtils.getCurrentRealName());
        application.setApproveTime(LocalDateTime.now());
        application.setRejectReason(rejectReason);

        borrowMapper.updateById(application);
        log.info("借阅申请被拒绝: id={}", id);
    }

    @Override
    @Transactional
    public void lend(Long id) {
        BorrowApplication application = getById(id);

        if (!BorrowApplication.STATUS_APPROVED.equals(application.getStatus())) {
            throw new BusinessException("该申请未审批通过");
        }

        application.setStatus(BorrowApplication.STATUS_BORROWED);
        application.setBorrowTime(LocalDateTime.now());
        borrowMapper.updateById(application);

        // 更新档案状态
        Archive archive = archiveMapper.selectById(application.getArchiveId());
        if (archive != null) {
            archive.setStatus(Archive.STATUS_BORROWED);
            archiveMapper.updateById(archive);
        }

        log.info("档案借出: applicationId={}, archiveId={}", id, application.getArchiveId());
    }

    @Override
    @Transactional
    public void returnArchive(Long id, String remarks) {
        BorrowApplication application = getById(id);

        if (!BorrowApplication.STATUS_BORROWED.equals(application.getStatus())) {
            throw new BusinessException("该申请不是借出状态");
        }

        application.setStatus(BorrowApplication.STATUS_RETURNED);
        application.setActualReturnDate(LocalDate.now());
        application.setReturnRemarks(remarks);
        borrowMapper.updateById(application);

        // 恢复档案状态
        Archive archive = archiveMapper.selectById(application.getArchiveId());
        if (archive != null) {
            archive.setStatus(Archive.STATUS_STORED);
            archiveMapper.updateById(archive);
        }

        log.info("档案归还: applicationId={}, archiveId={}", id, application.getArchiveId());
    }

    @Override
    @Transactional
    public void renew(Long id, LocalDate newReturnDate) {
        BorrowApplication application = getById(id);

        if (!BorrowApplication.STATUS_BORROWED.equals(application.getStatus())) {
            throw new BusinessException("只有借出中的档案可以续借");
        }

        if (newReturnDate.isBefore(application.getExpectedReturnDate())) {
            throw new BusinessException("新的归还日期不能早于原定日期");
        }

        application.setExpectedReturnDate(newReturnDate);
        application.setRenewCount(application.getRenewCount() != null ? application.getRenewCount() + 1 : 1);
        borrowMapper.updateById(application);

        log.info("档案续借: id={}, newReturnDate={}", id, newReturnDate);
    }

    @Override
    public List<BorrowApplication> getOverdueList() {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getStatus, BorrowApplication.STATUS_BORROWED)
               .lt(BorrowApplication::getExpectedReturnDate, LocalDate.now())
               .eq(BorrowApplication::getDeleted, false)
               .orderByAsc(BorrowApplication::getExpectedReturnDate);

        return borrowMapper.selectList(wrapper);
    }

    @Override
    public BorrowApplication getCurrentByArchiveId(Long archiveId) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getArchiveId, archiveId)
               .in(BorrowApplication::getStatus, 
                   BorrowApplication.STATUS_PENDING, 
                   BorrowApplication.STATUS_APPROVED,
                   BorrowApplication.STATUS_BORROWED)
               .eq(BorrowApplication::getDeleted, false)
               .last("LIMIT 1");

        return borrowMapper.selectOne(wrapper);
    }

    private String generateApplicationNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = applicationNoCounter.getAndIncrement();
        return String.format("BR-%s-%04d", date, seq);
    }
}
