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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 借阅服务实现.
 * @author junyuzhan
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
    public BorrowApplication apply(Long archiveId, String borrowPurpose, String borrowType, LocalDate expectedReturnDate, String remarks) {
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
        if (!Archive.STATUS_STORED.equals(archive.getStatus())) {
            throw new BusinessException("仅已归档档案可申请借阅");
        }
        if (BorrowApplication.TYPE_ONLINE.equals(borrowType) || BorrowApplication.TYPE_DOWNLOAD.equals(borrowType)) {
            if (!Boolean.TRUE.equals(archive.getHasElectronic())
                    && !Archive.FORM_ELECTRONIC.equals(archive.getArchiveForm())
                    && !Archive.FORM_HYBRID.equals(archive.getArchiveForm())) {
                throw new BusinessException("该档案无电子载体，不支持在线查阅或下载型借阅");
            }
        }
        if (BorrowApplication.TYPE_COPY.equals(borrowType)) {
            if (!Boolean.TRUE.equals(archive.getHasPhysical())
                    && !Archive.FORM_PHYSICAL.equals(archive.getArchiveForm())
                    && !Archive.FORM_HYBRID.equals(archive.getArchiveForm())) {
                throw new BusinessException("该档案无纸质载体，不支持复制利用申请");
            }
        }
        if ((Archive.SECURITY_CONFIDENTIAL.equals(archive.getSecurityLevel())
                || Archive.SECURITY_SECRET.equals(archive.getSecurityLevel()))
                && BorrowApplication.TYPE_DOWNLOAD.equals(borrowType)) {
            throw new BusinessException("秘密和机密档案仅允许在线查阅，不允许下载型借阅");
        }
        int maxBorrowDays = resolveMaxBorrowDays(archive, borrowType);
        LocalDate maxReturnDate = LocalDate.now().plusDays(maxBorrowDays);
        if (expectedReturnDate.isAfter(maxReturnDate)) {
            throw new BusinessException("该借阅方式最长可申请 " + maxBorrowDays + " 天，请调整预计归还日期");
        }

        Long userId = SecurityUtils.getCurrentUserId();
        String userName = SecurityUtils.getCurrentRealName();
        String department = SecurityUtils.getCurrentDepartment();

        BorrowApplication application = BorrowApplication.builder()
                .applicationNo(generateApplicationNo())
                .archiveId(archiveId)
                .archiveNo(archive.getArchiveNo())
                .archiveTitle(archive.getTitle())
                .applicantId(userId)
                .applicantName(userName)
                .applicantDept(department)
                .borrowPurpose(borrowPurpose)
                .borrowType(StringUtils.hasText(borrowType) ? borrowType : BorrowApplication.TYPE_ONLINE)
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
        
        // 权限校验：只有申请人本人或管理员/档案员可以查看
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isOwner = Objects.equals(application.getApplicantId(), currentUserId);
        boolean isAdmin = SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_MANAGER");
        
        if (!isOwner && !isAdmin) {
            throw new BusinessException("无权查看此借阅申请");
        }
        
        return application;
    }

    @Override
    public PageResult<BorrowApplication> getMyApplications(Long userId, String status, String borrowType, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getApplicantId, userId)
               .eq(BorrowApplication::getDeleted, false);

        appendBorrowFilters(wrapper, status, borrowType, keyword);

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
        if (!Objects.equals(application.getApplicantId(), currentUserId)) {
            throw new BusinessException("只能取消自己的申请");
        }

        application.setStatus(BorrowApplication.STATUS_CANCELLED);
        borrowMapper.updateById(application);

        log.info("借阅申请取消: id={}", id);
    }

    @Override
    public PageResult<BorrowApplication> getPendingList(String borrowType, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getStatus, BorrowApplication.STATUS_PENDING)
                .eq(BorrowApplication::getDeleted, false);
        appendBorrowFilters(wrapper, null, borrowType, keyword);

        Page<BorrowApplication> page = new Page<>(pageNum, pageSize);
        wrapper.orderByAsc(BorrowApplication::getApplyTime);
        Page<BorrowApplication> result = borrowMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    @Override
    public PageResult<BorrowApplication> getApprovedList(String borrowType, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<BorrowApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowApplication::getStatus, BorrowApplication.STATUS_APPROVED)
                .eq(BorrowApplication::getDeleted, false);
        appendBorrowFilters(wrapper, null, borrowType, keyword);

        Page<BorrowApplication> page = new Page<>(pageNum, pageSize);
        wrapper.orderByAsc(BorrowApplication::getApproveTime);
        Page<BorrowApplication> result = borrowMapper.selectPage(page, wrapper);

        return PageResult.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    private void appendBorrowFilters(LambdaQueryWrapper<BorrowApplication> wrapper, String status, String borrowType, String keyword) {
        if (StringUtils.hasText(status)) {
            wrapper.eq(BorrowApplication::getStatus, status);
        }
        if (StringUtils.hasText(borrowType)) {
            wrapper.eq(BorrowApplication::getBorrowType, borrowType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(BorrowApplication::getApplicationNo, keyword)
                    .or().like(BorrowApplication::getArchiveNo, keyword)
                    .or().like(BorrowApplication::getArchiveTitle, keyword)
                    .or().like(BorrowApplication::getApplicantName, keyword)
                    .or().like(BorrowApplication::getApplicantDept, keyword));
        }
    }

    @Override
    @Transactional
    public void approve(Long id, String remarks) {
        // 检查申请是否存在
        BorrowApplication application = getById(id);
        
        // 使用条件更新防止并发重复审批
        // UPDATE ... SET status = 'APPROVED' WHERE id = ? AND status = 'PENDING'
        LambdaUpdateWrapper<BorrowApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BorrowApplication::getId, id)
                .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_PENDING)
                .set(BorrowApplication::getStatus, BorrowApplication.STATUS_APPROVED)
                .set(BorrowApplication::getApproverId, SecurityUtils.getCurrentUserId())
                .set(BorrowApplication::getApproverName, SecurityUtils.getCurrentRealName())
                .set(BorrowApplication::getApproveTime, LocalDateTime.now())
                .set(BorrowApplication::getApproveRemarks, remarks);
        
        int affected = borrowMapper.update(null, updateWrapper);
        if (affected == 0) {
            // 重新查询确认当前状态
            BorrowApplication current = borrowMapper.selectById(id);
            if (current == null) {
                throw new NotFoundException("借阅申请不存在");
            }
            throw new BusinessException("该申请不是待审批状态，当前状态：" + current.getStatus());
        }
        log.info("借阅申请审批通过: id={}, applicationNo={}", id, application.getApplicationNo());
    }

    @Override
    @Transactional
    public void reject(Long id, String rejectReason) {
        // 检查申请是否存在
        BorrowApplication application = getById(id);
        
        // 使用条件更新防止并发重复审批
        LambdaUpdateWrapper<BorrowApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BorrowApplication::getId, id)
                .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_PENDING)
                .set(BorrowApplication::getStatus, BorrowApplication.STATUS_REJECTED)
                .set(BorrowApplication::getApproverId, SecurityUtils.getCurrentUserId())
                .set(BorrowApplication::getApproverName, SecurityUtils.getCurrentRealName())
                .set(BorrowApplication::getApproveTime, LocalDateTime.now())
                .set(BorrowApplication::getRejectReason, rejectReason);
        
        int affected = borrowMapper.update(null, updateWrapper);
        if (affected == 0) {
            BorrowApplication current = borrowMapper.selectById(id);
            if (current == null) {
                throw new NotFoundException("借阅申请不存在");
            }
            throw new BusinessException("该申请不是待审批状态，当前状态：" + current.getStatus());
        }
        log.info("借阅申请被拒绝: id={}, applicationNo={}", id, application.getApplicationNo());
    }

    @Override
    @Transactional
    public void lend(Long id) {
        // 检查申请是否存在
        BorrowApplication application = getById(id);
        
        // 使用条件更新防止重复借出
        LambdaUpdateWrapper<BorrowApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BorrowApplication::getId, id)
                .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_APPROVED)
                .set(BorrowApplication::getStatus, BorrowApplication.STATUS_BORROWED)
                .set(BorrowApplication::getBorrowTime, LocalDateTime.now());
        
        int affected = borrowMapper.update(null, updateWrapper);
        if (affected == 0) {
            BorrowApplication current = borrowMapper.selectById(id);
            if (current == null) {
                throw new NotFoundException("借阅申请不存在");
            }
            throw new BusinessException("该申请不是已审批状态或已被借出，当前状态：" + current.getStatus());
        }

        // 使用条件更新档案状态（只有当档案状态为 STORED 时才更新为 BORROWED）
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Archive> archiveUpdateWrapper = 
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        archiveUpdateWrapper.eq(Archive::getId, application.getArchiveId())
                .eq(Archive::getStatus, Archive.STATUS_STORED)
                .set(Archive::getStatus, Archive.STATUS_BORROWED);
        int archiveAffected = archiveMapper.update(null, archiveUpdateWrapper);
        if (archiveAffected == 0) {
            throw new BusinessException("档案状态已变更，借出失败，请刷新后重试");
        }

        log.info("档案借出: applicationId={}, archiveId={}", id, application.getArchiveId());
    }

    @Override
    @Transactional
    public void returnArchive(Long id, String remarks) {
        // 检查申请是否存在
        BorrowApplication application = getById(id);
        
        // 使用条件更新防止重复归还
        LambdaUpdateWrapper<BorrowApplication> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BorrowApplication::getId, id)
                .eq(BorrowApplication::getStatus, BorrowApplication.STATUS_BORROWED)
                .set(BorrowApplication::getStatus, BorrowApplication.STATUS_RETURNED)
                .set(BorrowApplication::getActualReturnDate, LocalDate.now())
                .set(BorrowApplication::getReturnRemarks, remarks);
        
        int affected = borrowMapper.update(null, updateWrapper);
        if (affected == 0) {
            BorrowApplication current = borrowMapper.selectById(id);
            if (current == null) {
                throw new NotFoundException("借阅申请不存在");
            }
            throw new BusinessException("该申请不是借出状态或已归还，当前状态：" + current.getStatus());
        }

        // 使用条件更新恢复档案状态（只有当档案状态为 BORROWED 时才更新为 STORED）
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Archive> archiveUpdateWrapper = 
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        archiveUpdateWrapper.eq(Archive::getId, application.getArchiveId())
                .eq(Archive::getStatus, Archive.STATUS_BORROWED)
                .set(Archive::getStatus, Archive.STATUS_STORED);
        int archiveAffected = archiveMapper.update(null, archiveUpdateWrapper);
        if (archiveAffected == 0) {
            throw new BusinessException("档案状态已变更，归还失败，请刷新后重试");
        }

        log.info("档案归还: applicationId={}, archiveId={}", id, application.getArchiveId());
    }

    @Override
    @Transactional
    public void renew(Long id, LocalDate newReturnDate) {
        BorrowApplication application = getById(id);

        // 【安全检查】只有申请人本人或管理员可以续借
        Long currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasAnyRole("SYSTEM_ADMIN", "ARCHIVE_MANAGER");
        if (!isAdmin && !Objects.equals(application.getApplicantId(), currentUserId)) {
            throw new BusinessException("只能续借自己的借阅申请");
        }

        if (!BorrowApplication.STATUS_BORROWED.equals(application.getStatus())) {
            throw new BusinessException("只有借出中的档案可以续借");
        }

        if (newReturnDate.isBefore(application.getExpectedReturnDate())) {
            throw new BusinessException("新的归还日期不能早于原定日期");
        }

        application.setExpectedReturnDate(newReturnDate);
        application.setRenewCount(application.getRenewCount() != null ? application.getRenewCount() + 1 : 1);
        borrowMapper.updateById(application);

        log.info("档案续借: id={}, newReturnDate={}, operator={}", id, newReturnDate, currentUserId);
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

    private int resolveMaxBorrowDays(Archive archive, String borrowType) {
        boolean sensitive = Archive.SECURITY_CONFIDENTIAL.equals(archive.getSecurityLevel())
                || Archive.SECURITY_SECRET.equals(archive.getSecurityLevel());
        return switch (borrowType) {
            case BorrowApplication.TYPE_DOWNLOAD -> 7;
            case BorrowApplication.TYPE_COPY -> 15;
            case BorrowApplication.TYPE_ONLINE -> sensitive ? 7 : 30;
            default -> 7;
        };
    }
}
