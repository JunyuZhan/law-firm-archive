package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateFeeCommand;
import com.lawfirm.application.finance.command.CreatePaymentCommand;
import com.lawfirm.application.finance.dto.FeeDTO;
import com.lawfirm.application.finance.dto.FeeQueryDTO;
import com.lawfirm.application.finance.dto.PaymentDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 收费管理应用服务
 * 
 * Requirements: 3.1, 3.2, 3.3 - 收款登记与数据锁定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeAppService {

    private final FeeRepository feeRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;
    private final CommissionAppService commissionAppService;

    /**
     * 分页查询收费记录
     */
    public PageResult<FeeDTO> listFees(FeeQueryDTO query) {
        LambdaQueryWrapper<Fee> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getFeeNo())) {
            wrapper.like(Fee::getFeeNo, query.getFeeNo());
        }
        if (query.getContractId() != null) {
            wrapper.eq(Fee::getContractId, query.getContractId());
        }
        if (query.getMatterId() != null) {
            wrapper.eq(Fee::getMatterId, query.getMatterId());
        }
        if (query.getClientId() != null) {
            wrapper.eq(Fee::getClientId, query.getClientId());
        }
        if (StringUtils.hasText(query.getFeeType())) {
            wrapper.eq(Fee::getFeeType, query.getFeeType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Fee::getStatus, query.getStatus());
        }
        if (query.getPlannedDateFrom() != null) {
            wrapper.ge(Fee::getPlannedDate, query.getPlannedDateFrom());
        }
        if (query.getPlannedDateTo() != null) {
            wrapper.le(Fee::getPlannedDate, query.getPlannedDateTo());
        }
        
        wrapper.orderByDesc(Fee::getCreatedAt);

        IPage<Fee> page = feeRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<FeeDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建收费记录
     */
    @Transactional
    public FeeDTO createFee(CreateFeeCommand command) {
        // 验证客户存在
        clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 生成收费编号
        String feeNo = generateFeeNo();

        Fee fee = Fee.builder()
                .feeNo(feeNo)
                .contractId(command.getContractId())
                .matterId(command.getMatterId())
                .clientId(command.getClientId())
                .feeType(command.getFeeType())
                .feeName(command.getFeeName())
                .amount(command.getAmount())
                .paidAmount(BigDecimal.ZERO)
                .currency(command.getCurrency() != null ? command.getCurrency() : "CNY")
                .plannedDate(command.getPlannedDate())
                .status("PENDING")
                .responsibleId(command.getResponsibleId() != null ? command.getResponsibleId() : SecurityUtils.getUserId())
                .remark(command.getRemark())
                .build();

        feeRepository.save(fee);
        log.info("收费记录创建成功: {}", fee.getFeeNo());
        return toDTO(fee);
    }

    /**
     * 获取收费详情
     */
    public FeeDTO getFeeById(Long id) {
        Fee fee = feeRepository.getByIdOrThrow(id, "收费记录不存在");
        FeeDTO dto = toDTO(fee);
        
        // 加载收款记录
        List<Payment> payments = paymentRepository.findByFeeId(id);
        dto.setPayments(payments.stream().map(this::toPaymentDTO).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 创建收款记录
     */
    @Transactional
    public PaymentDTO createPayment(CreatePaymentCommand command) {
        Fee fee = feeRepository.getByIdOrThrow(command.getFeeId(), "收费记录不存在");

        // 检查收款金额
        BigDecimal unpaid = fee.getAmount().subtract(fee.getPaidAmount());
        if (command.getAmount().compareTo(unpaid) > 0) {
            throw new BusinessException("收款金额超过待收金额");
        }

        String paymentNo = generatePaymentNo();

        Payment payment = Payment.builder()
                .paymentNo(paymentNo)
                .feeId(command.getFeeId())
                .contractId(fee.getContractId())
                .matterId(fee.getMatterId())
                .clientId(fee.getClientId())
                .amount(command.getAmount())
                .currency(command.getCurrency() != null ? command.getCurrency() : fee.getCurrency())
                .paymentMethod(command.getPaymentMethod())
                .paymentDate(command.getPaymentDate())
                .bankAccount(command.getBankAccount())
                .transactionNo(command.getTransactionNo())
                .status("PENDING")
                .remark(command.getRemark())
                .build();

        paymentRepository.save(payment);
        log.info("收款记录创建成功: {}", payment.getPaymentNo());
        return toPaymentDTO(payment);
    }

    /**
     * 确认收款
     * 收款确认后自动触发提成计算（M4-035）
     * 
     * Requirements: 3.1, 3.2, 3.3 - 收款登记后数据锁定
     */
    @Transactional
    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.getByIdOrThrow(paymentId, "收款记录不存在");
        
        if (!"PENDING".equals(payment.getStatus())) {
            throw new BusinessException("当前状态不允许确认");
        }

        payment.setStatus("CONFIRMED");
        payment.setConfirmerId(SecurityUtils.getUserId());
        
        // Requirements: 3.2 - 收款登记后自动锁定
        payment.setLocked(true);
        payment.setLockedAt(LocalDateTime.now());
        payment.setLockedBy(SecurityUtils.getUserId());
        
        paymentRepository.updateById(payment);

        // 更新收费记录的已收金额
        Fee fee = feeRepository.getByIdOrThrow(payment.getFeeId(), "收费记录不存在");
        BigDecimal newPaidAmount = fee.getPaidAmount().add(payment.getAmount());
        fee.setPaidAmount(newPaidAmount);
        fee.setActualDate(payment.getPaymentDate());
        
        // 更新收费状态
        if (newPaidAmount.compareTo(fee.getAmount()) >= 0) {
            fee.setStatus("PAID");
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            fee.setStatus("PARTIAL");
        }
        feeRepository.updateById(fee);

        // Requirements: 3.3 - 更新合同已收金额
        if (fee.getContractId() != null) {
            updateContractPaidAmount(fee.getContractId());
        }

        log.info("收款确认成功并已锁定: {}", payment.getPaymentNo());
        
        // 提成计算改为财务手动计算，不再自动触发
    }

    /**
     * 检查收款记录是否已锁定
     * 
     * Requirements: 3.2
     */
    public boolean isPaymentLocked(Long paymentId) {
        Payment payment = paymentRepository.getByIdOrThrow(paymentId, "收款记录不存在");
        return Boolean.TRUE.equals(payment.getLocked());
    }

    /**
     * 更新收款金额（仅限未锁定记录）
     * 
     * Requirements: 3.2 - 已锁定记录禁止直接修改
     */
    @Transactional
    public void updatePaymentAmount(Long paymentId, BigDecimal newAmount) {
        Payment payment = paymentRepository.getByIdOrThrow(paymentId, "收款记录不存在");
        
        // 检查是否已锁定
        if (Boolean.TRUE.equals(payment.getLocked())) {
            throw new BusinessException("收款记录已锁定，不能直接修改。如需修改，请提交变更申请");
        }
        
        // 检查状态
        if (!"PENDING".equals(payment.getStatus())) {
            throw new BusinessException("只有待确认状态的收款记录可以修改");
        }
        
        payment.setAmount(newAmount);
        paymentRepository.updateById(payment);
        log.info("收款金额更新成功: paymentId={}, newAmount={}", paymentId, newAmount);
    }

    /**
     * 取消收款
     */
    @Transactional
    public void cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.getByIdOrThrow(paymentId, "收款记录不存在");
        
        if ("CONFIRMED".equals(payment.getStatus())) {
            throw new BusinessException("已确认的收款不能取消");
        }

        payment.setStatus("CANCELLED");
        paymentRepository.updateById(payment);
        log.info("收款取消成功: {}", payment.getPaymentNo());
    }

    /**
     * 更新合同已收金额
     */
    private void updateContractPaidAmount(Long contractId) {
        BigDecimal totalPaid = feeRepository.sumPaidAmountByContractId(contractId);
        Contract contract = contractRepository.findById(contractId);
        if (contract != null) {
            contract.setPaidAmount(totalPaid);
            contractRepository.updateById(contract);
        }
    }

    /**
     * 生成收费编号
     */
    private String generateFeeNo() {
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "SF" + datePart + random;
    }

    /**
     * 生成收款编号
     */
    private String generatePaymentNo() {
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "SK" + datePart + random;
    }

    /**
     * 获取收费类型名称
     */
    private String getFeeTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "LAWYER_FEE" -> "律师费";
            case "COURT_FEE" -> "诉讼费";
            case "TRAVEL" -> "差旅费";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待收款";
            case "PARTIAL" -> "部分收款";
            case "PAID" -> "已收款";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * 获取收款方式名称
     */
    private String getPaymentMethodName(String method) {
        if (method == null) return null;
        return switch (method) {
            case "BANK" -> "银行转账";
            case "CASH" -> "现金";
            case "CHECK" -> "支票";
            case "OTHER" -> "其他";
            default -> method;
        };
    }

    /**
     * 获取收款状态名称
     */
    private String getPaymentStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待确认";
            case "CONFIRMED" -> "已确认";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * Fee Entity 转 DTO
     */
    private FeeDTO toDTO(Fee fee) {
        FeeDTO dto = new FeeDTO();
        dto.setId(fee.getId());
        dto.setFeeNo(fee.getFeeNo());
        dto.setContractId(fee.getContractId());
        dto.setMatterId(fee.getMatterId());
        dto.setClientId(fee.getClientId());
        dto.setFeeType(fee.getFeeType());
        dto.setFeeTypeName(getFeeTypeName(fee.getFeeType()));
        dto.setFeeName(fee.getFeeName());
        dto.setAmount(fee.getAmount());
        dto.setPaidAmount(fee.getPaidAmount());
        dto.setUnpaidAmount(fee.getAmount().subtract(fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO));
        dto.setCurrency(fee.getCurrency());
        dto.setPlannedDate(fee.getPlannedDate());
        dto.setActualDate(fee.getActualDate());
        dto.setStatus(fee.getStatus());
        dto.setStatusName(getStatusName(fee.getStatus()));
        dto.setResponsibleId(fee.getResponsibleId());
        dto.setRemark(fee.getRemark());
        dto.setCreatedAt(fee.getCreatedAt());
        dto.setUpdatedAt(fee.getUpdatedAt());
        return dto;
    }

    /**
     * Payment Entity 转 DTO
     */
    private PaymentDTO toPaymentDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setPaymentNo(payment.getPaymentNo());
        dto.setFeeId(payment.getFeeId());
        dto.setContractId(payment.getContractId());
        dto.setMatterId(payment.getMatterId());
        dto.setClientId(payment.getClientId());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentMethodName(getPaymentMethodName(payment.getPaymentMethod()));
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setBankAccount(payment.getBankAccount());
        dto.setTransactionNo(payment.getTransactionNo());
        dto.setStatus(payment.getStatus());
        dto.setStatusName(getPaymentStatusName(payment.getStatus()));
        dto.setConfirmerId(payment.getConfirmerId());
        dto.setRemark(payment.getRemark());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setUpdatedAt(payment.getUpdatedAt());
        return dto;
    }
}

