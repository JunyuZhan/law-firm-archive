package com.lawfirm.application.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.archive.command.CreateBorrowCommand;
import com.lawfirm.application.archive.command.ReturnArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveBorrowDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.archive.entity.Archive;
import com.lawfirm.domain.archive.entity.ArchiveBorrow;
import com.lawfirm.domain.archive.entity.ArchiveOperationLog;
import com.lawfirm.domain.archive.repository.ArchiveBorrowRepository;
import com.lawfirm.domain.archive.repository.ArchiveOperationLogRepository;
import com.lawfirm.domain.archive.repository.ArchiveRepository;
import com.lawfirm.infrastructure.persistence.mapper.ArchiveBorrowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.lawfirm.common.constant.ArchiveBorrowStatus;

/**
 * 档案借阅应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveBorrowAppService {

    private final ArchiveBorrowRepository borrowRepository;
    private final ArchiveBorrowMapper borrowMapper;
    private final ArchiveRepository archiveRepository;
    private final ArchiveOperationLogRepository operationLogRepository;

    /**
     * 分页查询借阅记录
     */
    public PageResult<ArchiveBorrowDTO> listBorrows(PageQuery query, Long archiveId, String status) {
        LambdaQueryWrapper<ArchiveBorrow> wrapper = new LambdaQueryWrapper<>();
        
        if (archiveId != null) {
            wrapper.eq(ArchiveBorrow::getArchiveId, archiveId);
        }
        if (status != null) {
            wrapper.eq(ArchiveBorrow::getStatus, status);
        }
        
        wrapper.orderByDesc(ArchiveBorrow::getCreatedAt);

        IPage<ArchiveBorrow> page = borrowRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<ArchiveBorrowDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建借阅申请
     */
    @Transactional
    public ArchiveBorrowDTO createBorrow(CreateBorrowCommand command) {
        Archive archive = archiveRepository.getByIdOrThrow(command.getArchiveId(), "档案不存在");
        
        if (!ArchiveBorrowStatus.ARCHIVE_STORED.equals(archive.getStatus())) {
            throw new BusinessException("只有已入库的档案才能申请借阅");
        }

        // 检查是否有未归还的借阅
        if (borrowRepository.count(
                new LambdaQueryWrapper<ArchiveBorrow>()
                        .eq(ArchiveBorrow::getArchiveId, command.getArchiveId())
                        .in(ArchiveBorrow::getStatus, List.of(ArchiveBorrowStatus.PENDING, ArchiveBorrowStatus.APPROVED, ArchiveBorrowStatus.BORROWED, ArchiveBorrowStatus.OVERDUE))) > 0) {
            throw new BusinessException("该档案已有未归还的借阅记录");
        }

        // 生成借阅编号
        String borrowNo = generateBorrowNo();

        ArchiveBorrow borrow = ArchiveBorrow.builder()
                .borrowNo(borrowNo)
                .archiveId(command.getArchiveId())
                .borrowerId(SecurityUtils.getUserId())
                .borrowReason(command.getBorrowReason())
                .borrowDate(command.getBorrowDate())
                .expectedReturnDate(command.getExpectedReturnDate())
                .status(ArchiveBorrowStatus.PENDING)
                .build();

        borrowRepository.save(borrow);
        log.info("借阅申请创建成功: {}", borrowNo);
        return toDTO(borrow);
    }

    /**
     * 审批借阅申请
     */
    @Transactional
    public void approveBorrow(Long borrowId) {
        ArchiveBorrow borrow = borrowRepository.getByIdOrThrow(borrowId, "借阅申请不存在");
        
        if (!ArchiveBorrowStatus.PENDING.equals(borrow.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        borrow.setStatus(ArchiveBorrowStatus.APPROVED);
        borrow.setApproverId(SecurityUtils.getUserId());
        borrow.setApprovedAt(LocalDateTime.now());
        borrowRepository.updateById(borrow);

        log.info("借阅申请审批通过: {}", borrow.getBorrowNo());
    }

    /**
     * 拒绝借阅申请
     */
    @Transactional
    public void rejectBorrow(Long borrowId, String reason) {
        ArchiveBorrow borrow = borrowRepository.getByIdOrThrow(borrowId, "借阅申请不存在");
        
        if (!ArchiveBorrowStatus.PENDING.equals(borrow.getStatus())) {
            throw new BusinessException("当前状态不允许拒绝");
        }

        borrow.setStatus(ArchiveBorrowStatus.REJECTED);
        borrow.setApproverId(SecurityUtils.getUserId());
        borrow.setApprovedAt(LocalDateTime.now());
        borrow.setRejectionReason(reason);
        borrowRepository.updateById(borrow);

        log.info("借阅申请已拒绝: {}", borrow.getBorrowNo());
    }

    /**
     * 确认借出
     */
    @Transactional
    public void confirmBorrow(Long borrowId) {
        ArchiveBorrow borrow = borrowRepository.getByIdOrThrow(borrowId, "借阅申请不存在");
        
        if (!ArchiveBorrowStatus.APPROVED.equals(borrow.getStatus())) {
            throw new BusinessException("只有已批准的申请才能借出");
        }

        Archive archive = archiveRepository.getByIdOrThrow(borrow.getArchiveId(), "档案不存在");
        
        borrow.setStatus(ArchiveBorrowStatus.BORROWED);
        borrowRepository.updateById(borrow);

        archive.setStatus(ArchiveBorrowStatus.ARCHIVE_BORROWED);
        archiveRepository.updateById(archive);

        // 记录操作日志
        logOperation(archive.getId(), "BORROW", "档案借出，借阅人：" + borrow.getBorrowerName(), SecurityUtils.getUserId());

        log.info("档案借出成功: {}", borrow.getBorrowNo());
    }

    /**
     * 归还档案
     */
    @Transactional
    public void returnArchive(ReturnArchiveCommand command) {
        ArchiveBorrow borrow = borrowRepository.getByIdOrThrow(command.getBorrowId(), "借阅记录不存在");
        
        if (!ArchiveBorrowStatus.BORROWED.equals(borrow.getStatus()) && !ArchiveBorrowStatus.OVERDUE.equals(borrow.getStatus())) {
            throw new BusinessException("只有借出或逾期的档案才能归还");
        }

        Archive archive = archiveRepository.getByIdOrThrow(borrow.getArchiveId(), "档案不存在");

        borrow.setStatus(ArchiveBorrowStatus.RETURNED);
        borrow.setActualReturnDate(LocalDate.now());
        borrow.setReturnHandlerId(SecurityUtils.getUserId());
        borrow.setReturnCondition(command.getReturnCondition());
        borrow.setReturnRemarks(command.getReturnRemarks());
        borrowRepository.updateById(borrow);

        archive.setStatus(ArchiveBorrowStatus.ARCHIVE_STORED);
        archiveRepository.updateById(archive);

        // 记录操作日志
        logOperation(archive.getId(), "RETURN", "档案归还，状态：" + command.getReturnCondition(), SecurityUtils.getUserId());

        log.info("档案归还成功: {}", borrow.getBorrowNo());
    }

    /**
     * 获取逾期借阅列表
     */
    public List<ArchiveBorrowDTO> getOverdueBorrows() {
        List<ArchiveBorrow> borrows = borrowMapper.selectOverdueBorrows(LocalDate.now());
        return borrows.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 生成借阅编号
     */
    private String generateBorrowNo() {
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = String.format("%04d", System.currentTimeMillis() % 10000);
        return "AB" + datePart + random;
    }

    /**
     * 记录操作日志
     */
    private void logOperation(Long archiveId, String operationType, String description, Long operatorId) {
        ArchiveOperationLog log = ArchiveOperationLog.builder()
                .archiveId(archiveId)
                .operationType(operationType)
                .operationDescription(description)
                .operatorId(operatorId)
                .operatedAt(LocalDateTime.now())
                .build();
        operationLogRepository.save(log);
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        return ArchiveBorrowStatus.getStatusName(status);
    }

    /**
     * 获取归还状态名称
     */
    private String getReturnConditionName(String condition) {
        return ArchiveBorrowStatus.getConditionName(condition);
    }

    /**
     * Entity 转 DTO
     */
    private ArchiveBorrowDTO toDTO(ArchiveBorrow borrow) {
        ArchiveBorrowDTO dto = new ArchiveBorrowDTO();
        dto.setId(borrow.getId());
        dto.setBorrowNo(borrow.getBorrowNo());
        dto.setArchiveId(borrow.getArchiveId());
        dto.setBorrowerId(borrow.getBorrowerId());
        dto.setBorrowerName(borrow.getBorrowerName());
        dto.setDepartment(borrow.getDepartment());
        dto.setBorrowReason(borrow.getBorrowReason());
        dto.setBorrowDate(borrow.getBorrowDate());
        dto.setExpectedReturnDate(borrow.getExpectedReturnDate());
        dto.setActualReturnDate(borrow.getActualReturnDate());
        dto.setStatus(borrow.getStatus());
        dto.setStatusName(getStatusName(borrow.getStatus()));
        dto.setApproverId(borrow.getApproverId());
        dto.setApprovedAt(borrow.getApprovedAt());
        dto.setRejectionReason(borrow.getRejectionReason());
        dto.setReturnHandlerId(borrow.getReturnHandlerId());
        dto.setReturnCondition(borrow.getReturnCondition());
        dto.setReturnConditionName(getReturnConditionName(borrow.getReturnCondition()));
        dto.setReturnRemarks(borrow.getReturnRemarks());
        dto.setIsOverdue(borrow.getExpectedReturnDate() != null && 
                         LocalDate.now().isAfter(borrow.getExpectedReturnDate()) &&
                         !ArchiveBorrowStatus.RETURNED.equals(borrow.getStatus()));
        dto.setCreatedAt(borrow.getCreatedAt());
        dto.setUpdatedAt(borrow.getUpdatedAt());
        return dto;
    }
}

