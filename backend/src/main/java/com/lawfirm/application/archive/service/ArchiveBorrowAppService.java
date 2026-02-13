package com.lawfirm.application.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.archive.command.CreateBorrowCommand;
import com.lawfirm.application.archive.command.ReturnArchiveCommand;
import com.lawfirm.application.archive.dto.ArchiveBorrowDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.constant.ArchiveBorrowStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 档案借阅应用服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveBorrowAppService {

  /** 随机数模数 */
  private static final int RANDOM_MOD = 10000;

  /** 日期子串起始位置 */
  private static final int DATE_SUBSTRING_START = 2;

  /** 格式化位数 */
  private static final int FORMAT_DIGITS = 4;

  /** 借阅仓储 */
  private final ArchiveBorrowRepository borrowRepository;

  /** 借阅Mapper */
  private final ArchiveBorrowMapper borrowMapper;

  /** 档案仓储 */
  private final ArchiveRepository archiveRepository;

  /** 操作日志仓储 */
  private final ArchiveOperationLogRepository operationLogRepository;

  /**
   * 分页查询借阅记录.
   *
   * @param query 分页查询条件
   * @param archiveId 档案ID
   * @param status 状态
   * @return 分页结果
   */
  public PageResult<ArchiveBorrowDTO> listBorrows(
      final PageQuery query, final Long archiveId, final String status) {
    LambdaQueryWrapper<ArchiveBorrow> wrapper = new LambdaQueryWrapper<>();

    if (archiveId != null) {
      wrapper.eq(ArchiveBorrow::getArchiveId, archiveId);
    }
    if (status != null) {
      wrapper.eq(ArchiveBorrow::getStatus, status);
    }

    wrapper.orderByDesc(ArchiveBorrow::getCreatedAt);

    IPage<ArchiveBorrow> page =
        borrowRepository.page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

    List<ArchiveBorrowDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 创建借阅申请.
   *
   * @param command 创建命令
   * @return 借阅DTO
   */
  @Transactional
  public ArchiveBorrowDTO createBorrow(final CreateBorrowCommand command) {
    Archive archive = archiveRepository.getByIdOrThrow(command.getArchiveId(), "档案不存在");

    if (!ArchiveBorrowStatus.ARCHIVE_STORED.equals(archive.getStatus())) {
      throw new BusinessException("只有已入库的档案才能申请借阅");
    }

    // 检查是否有未归还的借阅
    if (borrowRepository.count(
            new LambdaQueryWrapper<ArchiveBorrow>()
                .eq(ArchiveBorrow::getArchiveId, command.getArchiveId())
                .in(
                    ArchiveBorrow::getStatus,
                    List.of(
                        ArchiveBorrowStatus.PENDING,
                        ArchiveBorrowStatus.APPROVED,
                        ArchiveBorrowStatus.BORROWED,
                        ArchiveBorrowStatus.OVERDUE)))
        > 0) {
      throw new BusinessException("该档案已有未归还的借阅记录");
    }

    // 生成借阅编号
    String borrowNo = generateBorrowNo();

    ArchiveBorrow borrow =
        ArchiveBorrow.builder()
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
   * 审批借阅申请.
   *
   * @param borrowId 借阅ID
   */
  @Transactional
  public void approveBorrow(final Long borrowId) {
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
   * 拒绝借阅申请.
   *
   * @param borrowId 借阅ID
   * @param reason 拒绝原因
   */
  @Transactional
  public void rejectBorrow(final Long borrowId, final String reason) {
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
   * 确认借出.
   *
   * @param borrowId 借阅ID
   */
  @Transactional
  public void confirmBorrow(final Long borrowId) {
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
    logOperation(
        archive.getId(),
        "BORROW",
        "档案借出，借阅人：" + borrow.getBorrowerName(),
        SecurityUtils.getUserId());

    log.info("档案借出成功: {}", borrow.getBorrowNo());
  }

  /**
   * 归还档案.
   *
   * @param command 归还命令
   */
  @Transactional
  public void returnArchive(final ReturnArchiveCommand command) {
    ArchiveBorrow borrow = borrowRepository.getByIdOrThrow(command.getBorrowId(), "借阅记录不存在");

    if (!ArchiveBorrowStatus.BORROWED.equals(borrow.getStatus())
        && !ArchiveBorrowStatus.OVERDUE.equals(borrow.getStatus())) {
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
    logOperation(
        archive.getId(),
        "RETURN",
        "档案归还，状态：" + command.getReturnCondition(),
        SecurityUtils.getUserId());

    log.info("档案归还成功: {}", borrow.getBorrowNo());
  }

  /**
   * 获取逾期借阅列表.
   *
   * @return 逾期借阅列表
   */
  public List<ArchiveBorrowDTO> getOverdueBorrows() {
    List<ArchiveBorrow> borrows = borrowMapper.selectOverdueBorrows(LocalDate.now());
    return borrows.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 生成借阅编号.
   *
   * @return 借阅编号
   */
  private String generateBorrowNo() {
    String datePart = LocalDate.now().toString().replace("-", "").substring(DATE_SUBSTRING_START);
    String random =
        String.format("%0" + FORMAT_DIGITS + "d", System.currentTimeMillis() % RANDOM_MOD);
    return "AB" + datePart + random;
  }

  /**
   * 记录操作日志.
   *
   * @param archiveId 档案ID
   * @param operationType 操作类型
   * @param description 描述
   * @param operatorId 操作人ID
   */
  private void logOperation(
      final Long archiveId,
      final String operationType,
      final String description,
      final Long operatorId) {
    ArchiveOperationLog log =
        ArchiveOperationLog.builder()
            .archiveId(archiveId)
            .operationType(operationType)
            .operationDescription(description)
            .operatorId(operatorId)
            .operatedAt(LocalDateTime.now())
            .build();
    operationLogRepository.save(log);
  }

  /**
   * 获取状态名称.
   *
   * @param status 状态
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    return ArchiveBorrowStatus.getStatusName(status);
  }

  /**
   * 获取归还状态名称.
   *
   * @param condition 归还状态
   * @return 状态名称
   */
  private String getReturnConditionName(final String condition) {
    return ArchiveBorrowStatus.getConditionName(condition);
  }

  /**
   * Entity转DTO.
   *
   * @param borrow 实体对象
   * @return DTO对象
   */
  public ArchiveBorrowDTO toDTO(final ArchiveBorrow borrow) {
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
    dto.setIsOverdue(
        borrow.getExpectedReturnDate() != null
            && LocalDate.now().isAfter(borrow.getExpectedReturnDate())
            && !ArchiveBorrowStatus.RETURNED.equals(borrow.getStatus()));
    dto.setCreatedAt(borrow.getCreatedAt());
    dto.setUpdatedAt(borrow.getUpdatedAt());
    return dto;
  }
}
