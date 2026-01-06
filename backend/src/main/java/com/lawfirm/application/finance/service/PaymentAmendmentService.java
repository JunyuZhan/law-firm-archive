package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreatePaymentAmendmentCommand;
import com.lawfirm.application.finance.dto.PaymentAmendmentDTO;
import com.lawfirm.application.workbench.service.ApprovalService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收款变更申请服务
 * 用于已锁定收款记录的变更申请和审批流程
 * 
 * Requirements: 3.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAmendmentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAmendmentRepository amendmentRepository;
    private final FeeRepository feeRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;

    /**
     * 申请修改已锁定的收款记录
     * 
     * Requirements: 3.5
     */
    @Transactional
    public PaymentAmendmentDTO requestAmendment(CreatePaymentAmendmentCommand command) {
        Payment payment = paymentRepository.getByIdOrThrow(command.getPaymentId(), "收款记录不存在");
        
        // 检查是否已锁定
        if (!Boolean.TRUE.equals(payment.getLocked())) {
            throw new BusinessException("收款记录未锁定，可直接修改");
        }
        
        // 检查是否有待审批的变更申请
        List<PaymentAmendment> pendingAmendments = amendmentRepository.findByPaymentId(command.getPaymentId());
        boolean hasPending = pendingAmendments.stream()
            .anyMatch(a -> "PENDING".equals(a.getStatus()));
        if (hasPending) {
            throw new BusinessException("该收款记录已有待审批的变更申请");
        }
        
        // 创建变更申请
        PaymentAmendment amendment = PaymentAmendment.builder()
            .paymentId(command.getPaymentId())
            .originalAmount(payment.getAmount())
            .newAmount(command.getNewAmount())
            .reason(command.getReason())
            .requestedBy(SecurityUtils.getUserId())
            .requestedAt(LocalDateTime.now())
            .status("PENDING")
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
                null
            );
        } catch (Exception e) {
            log.warn("创建审批流程失败，变更申请仍然有效: {}", e.getMessage());
        }
        
        log.info("收款变更申请创建成功: paymentId={}, originalAmount={}, newAmount={}", 
            command.getPaymentId(), payment.getAmount(), command.getNewAmount());
        
        return toDTO(amendment);
    }

    /**
     * 审批通过变更申请
     * 
     * Requirements: 3.5
     */
    @Transactional
    public PaymentAmendmentDTO approveAmendment(Long amendmentId, String comment) {
        PaymentAmendment amendment = amendmentRepository.getByIdOrThrow(amendmentId, "变更申请不存在");
        
        if (!"PENDING".equals(amendment.getStatus())) {
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
                    fee.setStatus("PAID");
                } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                    fee.setStatus("PARTIAL");
                } else {
                    fee.setStatus("PENDING");
                }
                feeRepository.updateById(fee);
            }
        }
        
        // 更新合同已收金额
        if (payment.getContractId() != null) {
            updateContractPaidAmount(payment.getContractId(), diff);
        }
        
        // 更新变更申请状态
        amendment.setStatus("APPROVED");
        amendment.setApprovedBy(SecurityUtils.getUserId());
        amendment.setApprovedAt(LocalDateTime.now());
        amendmentRepository.updateById(amendment);
        
        log.info("收款变更审批通过: amendmentId={}, paymentId={}, 原金额={}, 新金额={}", 
            amendmentId, payment.getId(), amendment.getOriginalAmount(), amendment.getNewAmount());
        
        return toDTO(amendment);
    }

    /**
     * 拒绝变更申请
     * 
     * Requirements: 3.5
     */
    @Transactional
    public PaymentAmendmentDTO rejectAmendment(Long amendmentId, String rejectReason) {
        PaymentAmendment amendment = amendmentRepository.getByIdOrThrow(amendmentId, "变更申请不存在");
        
        if (!"PENDING".equals(amendment.getStatus())) {
            throw new BusinessException("变更申请状态不正确");
        }
        
        amendment.setStatus("REJECTED");
        amendment.setApprovedBy(SecurityUtils.getUserId());
        amendment.setApprovedAt(LocalDateTime.now());
        amendment.setRejectReason(rejectReason);
        amendmentRepository.updateById(amendment);
        
        log.info("收款变更申请被拒绝: amendmentId={}, reason={}", amendmentId, rejectReason);
        
        return toDTO(amendment);
    }

    /**
     * 查询变更申请列表
     */
    public PageResult<PaymentAmendmentDTO> listAmendments(int pageNum, int pageSize, String status) {
        LambdaQueryWrapper<PaymentAmendment> wrapper = new LambdaQueryWrapper<>();
        // @TableLogic 会自动处理 deleted 条件，无需手动添加
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(PaymentAmendment::getStatus, status);
        }
        
        wrapper.orderByDesc(PaymentAmendment::getRequestedAt);
        
        IPage<PaymentAmendment> page = amendmentRepository.page(
            new Page<>(pageNum, pageSize), wrapper);
        
        List<PaymentAmendmentDTO> records = page.getRecords().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        
        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 获取变更申请详情
     */
    public PaymentAmendmentDTO getAmendmentById(Long id) {
        PaymentAmendment amendment = amendmentRepository.getByIdOrThrow(id, "变更申请不存在");
        return toDTO(amendment);
    }

    /**
     * 查询收款记录的变更历史
     */
    public List<PaymentAmendmentDTO> getAmendmentsByPaymentId(Long paymentId) {
        List<PaymentAmendment> amendments = amendmentRepository.findByPaymentId(paymentId);
        return amendments.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 更新合同已收金额
     */
    private void updateContractPaidAmount(Long contractId, BigDecimal diff) {
        Contract contract = contractRepository.findById(contractId);
        if (contract != null) {
            BigDecimal newPaidAmount = contract.getPaidAmount().add(diff);
            contract.setPaidAmount(newPaidAmount);
            contractRepository.updateById(contract);
        }
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已批准";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    /**
     * 转换为DTO
     */
    private PaymentAmendmentDTO toDTO(PaymentAmendment amendment) {
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
