package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreatePaymentAmendmentCommand;
import com.lawfirm.application.finance.dto.PaymentAmendmentDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.common.constant.ApprovalStatus;
import com.lawfirm.common.constant.PaymentStatus;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.finance.entity.PaymentAmendment;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PaymentAmendmentRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收款变更申请服务 用于已锁定收款记录的变更申请和审批流程
 *
 * <p>Requirements: 3.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAmendmentService {

  /** 收款仓储. */
  private final PaymentRepository paymentRepository;

  /** 收款变更仓储. */
  private final PaymentAmendmentRepository amendmentRepository;

  /** 费用仓储. */
  private final FeeRepository feeRepository;

  /** 合同仓储. */
  private final ContractRepository contractRepository;

  /** 用户仓储. */
  private final UserRepository userRepository;

  /** 审批服务. */
  private final ApprovalService approvalService;

  /**
   * 申请修改已锁定的收款记录
   *
   * <p>Requirements: 3.5
   *
   * @param command 创建收款变更命令
   * @return 收款变更DTO
   */
  @Transactional
  public PaymentAmendmentDTO requestAmendment(final CreatePaymentAmendmentCommand command) {
    Payment payment = paymentRepository.getByIdOrThrow(command.getPaymentId(), "收款记录不存在");

    // 检查是否已锁定
    if (!Boolean.TRUE.equals(payment.getLocked())) {
      throw new BusinessException("收款记录未锁定，可直接修改");
    }

    // 检查是否有待审批的变更申请
    List<PaymentAmendment> pendingAmendments =
        amendmentRepository.findByPaymentId(command.getPaymentId());
    boolean hasPending =
        pendingAmendments.stream().anyMatch(a -> ApprovalStatus.PENDING.equals(a.getStatus()));
    if (hasPending) {
      throw new BusinessException("该收款记录已有待审批的变更申请");
    }

    // 创建变更申请
    PaymentAmendment amendment =
        PaymentAmendment.builder()
            .paymentId(command.getPaymentId())
            .originalAmount(payment.getAmount())
            .newAmount(command.getNewAmount())
            .reason(command.getReason())
            .requestedBy(SecurityUtils.getUserId())
            .requestedAt(LocalDateTime.now())
            .status(ApprovalStatus.PENDING)
            .build();

    amendmentRepository.save(amendment);

    // 创建审批流程
    try {
      approvalService.createApproval(
          "PAYMENT_AMENDMENT",
          amendment.getId(),
          "收款变更-" + payment.getPaymentNo(),
          "收款金额变更申请：" + payment.getAmount() + " → " + command.getNewAmount(),
          null, // 自动查找财务主管
          "HIGH",
          "NORMAL",
          null);
    } catch (Exception e) {
      log.warn("创建审批流程失败，变更申请仍然有效: {}", e.getMessage());
    }

    log.info(
        "收款变更申请创建成功: paymentId={}, originalAmount={}, newAmount={}",
        command.getPaymentId(),
        payment.getAmount(),
        command.getNewAmount());

    return toDTO(amendment);
  }

  /**
   * 审批通过变更申请
   *
   * <p>Requirements: 3.5
   *
   * @param amendmentId 变更申请ID
   * @param comment 审批意见
   * @return 收款变更DTO
   */
  @Transactional
  public PaymentAmendmentDTO approveAmendment(final Long amendmentId, final String comment) {
    PaymentAmendment amendment = amendmentRepository.getByIdOrThrow(amendmentId, "变更申请不存在");

    if (!ApprovalStatus.PENDING.equals(amendment.getStatus())) {
      throw new BusinessException("变更申请状态不正确");
    }

    // 更新收款记录金额
    Payment payment = paymentRepository.getByIdOrThrow(amendment.getPaymentId(), "收款记录不存在");
    BigDecimal diff = amendment.getNewAmount().subtract(amendment.getOriginalAmount());
    payment.setAmount(amendment.getNewAmount());
    paymentRepository.updateById(payment);

    // 更新收费记录的已收金额
    if (payment.getFeeId() != null) {
      Fee fee = feeRepository.findById(payment.getFeeId());
      if (fee != null) {
        BigDecimal newPaidAmount = fee.getPaidAmount().add(diff);
        fee.setPaidAmount(newPaidAmount);

        // 更新收费状态
        if (newPaidAmount.compareTo(fee.getAmount()) >= 0) {
          fee.setStatus(PaymentStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
          fee.setStatus(PaymentStatus.PARTIAL);
        } else {
          fee.setStatus(PaymentStatus.PENDING);
        }
        feeRepository.updateById(fee);
      }
    }

    // 更新合同已收金额
    if (payment.getContractId() != null) {
      updateContractPaidAmount(payment.getContractId(), diff);
    }

    // 更新变更申请状态
    amendment.setStatus(ApprovalStatus.APPROVED);
    amendment.setApprovedBy(SecurityUtils.getUserId());
    amendment.setApprovedAt(LocalDateTime.now());
    amendmentRepository.updateById(amendment);

    log.info(
        "收款变更审批通过: amendmentId={}, paymentId={}, 原金额={}, 新金额={}",
        amendmentId,
        payment.getId(),
        amendment.getOriginalAmount(),
        amendment.getNewAmount());

    return toDTO(amendment);
  }

  /**
   * 拒绝变更申请
   *
   * <p>Requirements: 3.5
   *
   * @param amendmentId 变更申请ID
   * @param rejectReason 拒绝原因
   * @return 收款变更DTO
   */
  @Transactional
  public PaymentAmendmentDTO rejectAmendment(final Long amendmentId, final String rejectReason) {
    PaymentAmendment amendment = amendmentRepository.getByIdOrThrow(amendmentId, "变更申请不存在");

    if (!ApprovalStatus.PENDING.equals(amendment.getStatus())) {
      throw new BusinessException("变更申请状态不正确");
    }

    amendment.setStatus(ApprovalStatus.REJECTED);
    amendment.setApprovedBy(SecurityUtils.getUserId());
    amendment.setApprovedAt(LocalDateTime.now());
    amendment.setRejectReason(rejectReason);
    amendmentRepository.updateById(amendment);

    log.info("收款变更申请被拒绝: amendmentId={}, reason={}", amendmentId, rejectReason);

    return toDTO(amendment);
  }

  /**
   * 查询变更申请列表 数据范围：ADMIN/DIRECTOR/FINANCE 可看全部，其他人只能看自己申请的
   *
   * @param pageNum 页码
   * @param pageSize 每页大小
   * @param status 状态
   * @return 变更申请分页结果
   */
  public PageResult<PaymentAmendmentDTO> listAmendments(
      final int pageNum, final int pageSize, final String status) {
    LambdaQueryWrapper<PaymentAmendment> wrapper = new LambdaQueryWrapper<>();
    // @TableLogic 会自动处理 deleted 条件，无需手动添加

    // 数据范围过滤
    Long currentUserId = SecurityUtils.getUserId();
    if (!canViewAllAmendments()) {
      // 非管理层/财务只能看自己申请的
      wrapper.eq(PaymentAmendment::getRequestedBy, currentUserId);
    }

    if (status != null && !status.isEmpty()) {
      wrapper.eq(PaymentAmendment::getStatus, status);
    }

    wrapper.orderByDesc(PaymentAmendment::getRequestedAt);

    IPage<PaymentAmendment> page = amendmentRepository.page(new Page<>(pageNum, pageSize), wrapper);

    List<PaymentAmendmentDTO> records =
        page.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(records, page.getTotal(), pageNum, pageSize);
  }

  /**
   * 检查是否可以查看所有变更申请（ADMIN/DIRECTOR/FINANCE）
   *
   * @return 是否可以查看所有变更申请
   */
  private boolean canViewAllAmendments() {
    if (SecurityUtils.isAdmin()) {
      return true;
    }
    Long userId = SecurityUtils.getUserId();
    List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
    return roleCodes.contains("DIRECTOR") || roleCodes.contains("FINANCE");
  }

  /**
   * 获取变更申请详情
   *
   * @param id 变更申请ID
   * @return 收款变更DTO
   */
  public PaymentAmendmentDTO getAmendmentById(final Long id) {
    PaymentAmendment amendment = amendmentRepository.getByIdOrThrow(id, "变更申请不存在");
    return toDTO(amendment);
  }

  /**
   * 查询收款记录的变更历史
   *
   * @param paymentId 收款记录ID
   * @return 变更申请列表
   */
  public List<PaymentAmendmentDTO> getAmendmentsByPaymentId(final Long paymentId) {
    List<PaymentAmendment> amendments = amendmentRepository.findByPaymentId(paymentId);
    return amendments.stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 更新合同已收金额
   *
   * @param contractId 合同ID
   * @param diff 金额差额
   */
  private void updateContractPaidAmount(final Long contractId, final BigDecimal diff) {
    Contract contract = contractRepository.findById(contractId);
    if (contract != null) {
      BigDecimal newPaidAmount = contract.getPaidAmount().add(diff);
      contract.setPaidAmount(newPaidAmount);
      contractRepository.updateById(contract);
    }
  }

  /**
   * 获取状态名称
   *
   * @param status 状态代码
   * @return 状态名称
   */
  private String getStatusName(final String status) {
    return ApprovalStatus.getStatusName(status);
  }

  /**
   * 转换为DTO
   *
   * @param amendment 收款变更实体
   * @return 收款变更DTO
   */
  private PaymentAmendmentDTO toDTO(final PaymentAmendment amendment) {
    PaymentAmendmentDTO dto = new PaymentAmendmentDTO();
    dto.setId(amendment.getId());
    dto.setPaymentId(amendment.getPaymentId());
    dto.setOriginalAmount(amendment.getOriginalAmount());
    dto.setNewAmount(amendment.getNewAmount());
    dto.setAmountDiff(amendment.getNewAmount().subtract(amendment.getOriginalAmount()));
    dto.setReason(amendment.getReason());
    dto.setRequestedBy(amendment.getRequestedBy());
    dto.setRequestedAt(amendment.getRequestedAt());
    dto.setApprovedBy(amendment.getApprovedBy());
    dto.setApprovedAt(amendment.getApprovedAt());
    dto.setStatus(amendment.getStatus());
    dto.setStatusName(getStatusName(amendment.getStatus()));
    dto.setRejectReason(amendment.getRejectReason());
    dto.setCreatedAt(amendment.getCreatedAt());
    dto.setUpdatedAt(amendment.getUpdatedAt());

    // 加载收款编号
    Payment payment = paymentRepository.findById(amendment.getPaymentId());
    if (payment != null) {
      dto.setPaymentNo(payment.getPaymentNo());
    }

    // 加载申请人姓名
    if (amendment.getRequestedBy() != null) {
      User requester = userRepository.findById(amendment.getRequestedBy());
      if (requester != null) {
        dto.setRequestedByName(requester.getRealName());
      }
    }

    // 加载审批人姓名
    if (amendment.getApprovedBy() != null) {
      User approver = userRepository.findById(amendment.getApprovedBy());
      if (approver != null) {
        dto.setApprovedByName(approver.getRealName());
      }
    }

    return dto;
  }
}
