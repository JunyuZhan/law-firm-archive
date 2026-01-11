package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreatePrepaymentCommand;
import com.lawfirm.application.finance.command.UsePrepaymentCommand;
import com.lawfirm.application.finance.dto.PrepaymentDTO;
import com.lawfirm.application.finance.dto.PrepaymentQueryDTO;
import com.lawfirm.application.finance.dto.PrepaymentUsageDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Prepayment;
import com.lawfirm.domain.finance.entity.PrepaymentUsage;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PrepaymentRepository;
import com.lawfirm.domain.finance.repository.PrepaymentUsageRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentMapper;
import com.lawfirm.infrastructure.persistence.mapper.PrepaymentUsageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 预收款应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrepaymentAppService {

    private final PrepaymentRepository prepaymentRepository;
    private final PrepaymentUsageRepository usageRepository;
    private final PrepaymentMapper prepaymentMapper;
    private final PrepaymentUsageMapper usageMapper;
    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;
    private final MatterRepository matterRepository;
    private final FeeRepository feeRepository;
    private final UserRepository userRepository;

    /**
     * 分页查询预收款
     */
    public PageResult<PrepaymentDTO> listPrepayments(PrepaymentQueryDTO query) {
        IPage<Prepayment> page = prepaymentMapper.selectPrepaymentPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                query.getClientId(),
                query.getMatterId(),
                query.getContractId(),
                query.getStatus(),
                query.getPrepaymentNo()
        );

        List<PrepaymentDTO> records = batchConvertToDTO(page.getRecords());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }
    
    /**
     * 批量转换预收款DTO（优化N+1查询）
     */
    private List<PrepaymentDTO> batchConvertToDTO(List<Prepayment> prepayments) {
        if (prepayments.isEmpty()) {
            return List.of();
        }
        
        // 收集所有需要查询的ID
        Set<Long> clientIds = new HashSet<>();
        Set<Long> contractIds = new HashSet<>();
        Set<Long> matterIds = new HashSet<>();
        Set<Long> confirmerIds = new HashSet<>();
        
        for (Prepayment p : prepayments) {
            if (p.getClientId() != null) clientIds.add(p.getClientId());
            if (p.getContractId() != null) contractIds.add(p.getContractId());
            if (p.getMatterId() != null) matterIds.add(p.getMatterId());
            if (p.getConfirmerId() != null) confirmerIds.add(p.getConfirmerId());
        }
        
        // 批量加载关联数据
        Map<Long, Client> clientMap = clientIds.isEmpty() ? Map.of() :
                clientRepository.listByIds(clientIds).stream()
                        .collect(Collectors.toMap(Client::getId, Function.identity()));
        
        Map<Long, Contract> contractMap = contractIds.isEmpty() ? Map.of() :
                contractRepository.listByIds(contractIds).stream()
                        .collect(Collectors.toMap(Contract::getId, Function.identity()));
        
        Map<Long, Matter> matterMap = matterIds.isEmpty() ? Map.of() :
                matterRepository.listByIds(matterIds).stream()
                        .collect(Collectors.toMap(Matter::getId, Function.identity()));
        
        Map<Long, User> userMap = confirmerIds.isEmpty() ? Map.of() :
                userRepository.listByIds(confirmerIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));
        
        // 转换DTO
        return prepayments.stream()
                .map(p -> toDTOWithMaps(p, clientMap, contractMap, matterMap, userMap))
                .collect(Collectors.toList());
    }

    /**
     * 获取预收款详情
     */
    public PrepaymentDTO getPrepaymentById(Long id) {
        Prepayment prepayment = prepaymentRepository.getByIdOrThrow(id, "预收款记录不存在");
        PrepaymentDTO dto = toDTO(prepayment);
        
        // 加载核销记录
        List<PrepaymentUsage> usages = usageRepository.findByPrepaymentId(id);
        dto.setUsages(usages.stream().map(this::toUsageDTO).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 创建预收款
     */
    @Transactional
    public PrepaymentDTO createPrepayment(CreatePrepaymentCommand command) {
        // 验证客户存在
        clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 验证合同存在（如果指定）
        if (command.getContractId() != null) {
            contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");
        }

        // 验证项目存在（如果指定）
        if (command.getMatterId() != null) {
            matterRepository.getByIdOrThrow(command.getMatterId(), "项目不存在");
        }

        String prepaymentNo = generatePrepaymentNo();

        Prepayment prepayment = Prepayment.builder()
                .prepaymentNo(prepaymentNo)
                .clientId(command.getClientId())
                .contractId(command.getContractId())
                .matterId(command.getMatterId())
                .amount(command.getAmount())
                .usedAmount(BigDecimal.ZERO)
                .remainingAmount(command.getAmount())
                .currency(command.getCurrency() != null ? command.getCurrency() : "CNY")
                .receiptDate(command.getReceiptDate())
                .paymentMethod(command.getPaymentMethod())
                .bankAccount(command.getBankAccount())
                .transactionNo(command.getTransactionNo())
                .status("PENDING")
                .purpose(command.getPurpose())
                .remark(command.getRemark())
                .build();

        prepaymentRepository.save(prepayment);
        log.info("预收款创建成功: {}", prepayment.getPrepaymentNo());
        return toDTO(prepayment);
    }

    /**
     * 确认预收款
     */
    @Transactional
    public PrepaymentDTO confirmPrepayment(Long id) {
        Prepayment prepayment = prepaymentRepository.getByIdOrThrow(id, "预收款记录不存在");
        
        if (!"PENDING".equals(prepayment.getStatus())) {
            throw new BusinessException("当前状态不允许确认");
        }

        prepayment.setStatus("ACTIVE");
        prepayment.setConfirmerId(SecurityUtils.getUserId());
        prepayment.setConfirmedAt(LocalDateTime.now());
        prepaymentRepository.updateById(prepayment);

        log.info("预收款确认成功: {}", prepayment.getPrepaymentNo());
        return toDTO(prepayment);
    }

    /**
     * 使用预收款（核销到收费记录）
     */
    @Transactional
    public PrepaymentUsageDTO usePrepayment(UsePrepaymentCommand command) {
        Prepayment prepayment = prepaymentRepository.getByIdOrThrow(command.getPrepaymentId(), "预收款记录不存在");
        Fee fee = feeRepository.getByIdOrThrow(command.getFeeId(), "收费记录不存在");

        // 验证预收款状态
        if (!"ACTIVE".equals(prepayment.getStatus())) {
            throw new BusinessException("预收款状态不允许使用");
        }

        // 验证剩余金额
        if (command.getAmount().compareTo(prepayment.getRemainingAmount()) > 0) {
            throw new BusinessException("核销金额超过预收款剩余金额");
        }

        // 验证收费记录待收金额
        BigDecimal unpaid = fee.getAmount().subtract(
                fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO);
        if (command.getAmount().compareTo(unpaid) > 0) {
            throw new BusinessException("核销金额超过收费记录待收金额");
        }

        // 验证客户一致性
        if (!prepayment.getClientId().equals(fee.getClientId())) {
            throw new BusinessException("预收款客户与收费记录客户不一致");
        }

        // 创建核销记录
        PrepaymentUsage usage = PrepaymentUsage.builder()
                .prepaymentId(command.getPrepaymentId())
                .feeId(command.getFeeId())
                .matterId(fee.getMatterId())
                .amount(command.getAmount())
                .usageTime(LocalDateTime.now())
                .operatorId(SecurityUtils.getUserId())
                .remark(command.getRemark())
                .build();
        usageRepository.save(usage);

        // 更新预收款金额
        BigDecimal newUsedAmount = prepayment.getUsedAmount().add(command.getAmount());
        BigDecimal newRemainingAmount = prepayment.getAmount().subtract(newUsedAmount);
        prepayment.setUsedAmount(newUsedAmount);
        prepayment.setRemainingAmount(newRemainingAmount);
        
        // 如果剩余金额为0，更新状态
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            prepayment.setStatus("USED");
        }
        prepaymentRepository.updateById(prepayment);

        // 更新收费记录已收金额
        BigDecimal newPaidAmount = (fee.getPaidAmount() != null ? fee.getPaidAmount() : BigDecimal.ZERO)
                .add(command.getAmount());
        fee.setPaidAmount(newPaidAmount);
        fee.setActualDate(LocalDate.now());
        
        // 更新收费状态
        if (newPaidAmount.compareTo(fee.getAmount()) >= 0) {
            fee.setStatus("PAID");
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            fee.setStatus("PARTIAL");
        }
        feeRepository.updateById(fee);

        log.info("预收款核销成功: prepaymentNo={}, feeNo={}, amount={}", 
                prepayment.getPrepaymentNo(), fee.getFeeNo(), command.getAmount());
        return toUsageDTO(usage);
    }

    /**
     * 查询客户可用预收款
     */
    public List<PrepaymentDTO> getAvailablePrepayments(Long clientId) {
        List<Prepayment> prepayments = prepaymentRepository.findActiveByClientId(clientId);
        return prepayments.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 退款
     */
    @Transactional
    public PrepaymentDTO refundPrepayment(Long id, String remark) {
        Prepayment prepayment = prepaymentRepository.getByIdOrThrow(id, "预收款记录不存在");
        
        if (!"ACTIVE".equals(prepayment.getStatus())) {
            throw new BusinessException("当前状态不允许退款");
        }

        if (prepayment.getUsedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("已有核销记录，不能直接退款");
        }

        prepayment.setStatus("REFUNDED");
        prepayment.setRemark((prepayment.getRemark() != null ? prepayment.getRemark() + "\n" : "") + 
                "退款原因: " + remark);
        prepaymentRepository.updateById(prepayment);

        log.info("预收款退款成功: {}", prepayment.getPrepaymentNo());
        return toDTO(prepayment);
    }

    /**
     * 生成预收款编号
     */
    private String generatePrepaymentNo() {
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "YS" + datePart + random;
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
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待确认";
            case "ACTIVE" -> "有效";
            case "USED" -> "已用完";
            case "REFUNDED" -> "已退款";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }

    /**
     * Prepayment Entity 转 DTO（用于单条记录查询）
     */
    private PrepaymentDTO toDTO(Prepayment prepayment) {
        return toDTOWithMaps(prepayment, null, null, null, null);
    }
    
    /**
     * Prepayment Entity 转 DTO（支持预加载的Map）
     */
    private PrepaymentDTO toDTOWithMaps(Prepayment prepayment, 
                                        Map<Long, Client> clientMap,
                                        Map<Long, Contract> contractMap,
                                        Map<Long, Matter> matterMap,
                                        Map<Long, User> userMap) {
        PrepaymentDTO dto = new PrepaymentDTO();
        dto.setId(prepayment.getId());
        dto.setPrepaymentNo(prepayment.getPrepaymentNo());
        dto.setClientId(prepayment.getClientId());
        dto.setContractId(prepayment.getContractId());
        dto.setMatterId(prepayment.getMatterId());
        dto.setAmount(prepayment.getAmount());
        dto.setUsedAmount(prepayment.getUsedAmount());
        dto.setRemainingAmount(prepayment.getRemainingAmount());
        dto.setCurrency(prepayment.getCurrency());
        dto.setReceiptDate(prepayment.getReceiptDate());
        dto.setPaymentMethod(prepayment.getPaymentMethod());
        dto.setPaymentMethodName(getPaymentMethodName(prepayment.getPaymentMethod()));
        dto.setBankAccount(prepayment.getBankAccount());
        dto.setTransactionNo(prepayment.getTransactionNo());
        dto.setStatus(prepayment.getStatus());
        dto.setStatusName(getStatusName(prepayment.getStatus()));
        dto.setConfirmerId(prepayment.getConfirmerId());
        dto.setConfirmedAt(prepayment.getConfirmedAt());
        dto.setPurpose(prepayment.getPurpose());
        dto.setRemark(prepayment.getRemark());
        dto.setCreatedAt(prepayment.getCreatedAt());
        dto.setUpdatedAt(prepayment.getUpdatedAt());

        // 关联信息 - 优先从Map获取，否则单独查询
        if (prepayment.getClientId() != null) {
            Client client = (clientMap != null) ? clientMap.get(prepayment.getClientId()) 
                    : clientRepository.findById(prepayment.getClientId());
            if (client != null) {
                dto.setClientName(client.getName());
            }
        }
        if (prepayment.getContractId() != null) {
            Contract contract = (contractMap != null) ? contractMap.get(prepayment.getContractId()) 
                    : contractRepository.findById(prepayment.getContractId());
            if (contract != null) {
                dto.setContractNo(contract.getContractNo());
            }
        }
        if (prepayment.getMatterId() != null) {
            Matter matter = (matterMap != null) ? matterMap.get(prepayment.getMatterId()) 
                    : matterRepository.findById(prepayment.getMatterId());
            if (matter != null) {
                dto.setMatterNo(matter.getMatterNo());
                dto.setMatterName(matter.getName());
            }
        }
        if (prepayment.getConfirmerId() != null) {
            User user = (userMap != null) ? userMap.get(prepayment.getConfirmerId()) 
                    : userRepository.findById(prepayment.getConfirmerId());
            if (user != null) {
                dto.setConfirmerName(user.getRealName());
            }
        }

        return dto;
    }

    /**
     * PrepaymentUsage Entity 转 DTO
     */
    private PrepaymentUsageDTO toUsageDTO(PrepaymentUsage usage) {
        PrepaymentUsageDTO dto = new PrepaymentUsageDTO();
        dto.setId(usage.getId());
        dto.setPrepaymentId(usage.getPrepaymentId());
        dto.setFeeId(usage.getFeeId());
        dto.setMatterId(usage.getMatterId());
        dto.setAmount(usage.getAmount());
        dto.setUsageTime(usage.getUsageTime());
        dto.setOperatorId(usage.getOperatorId());
        dto.setRemark(usage.getRemark());
        dto.setCreatedAt(usage.getCreatedAt());

        // 关联信息
        if (usage.getPrepaymentId() != null) {
            Prepayment prepayment = prepaymentRepository.findById(usage.getPrepaymentId());
            if (prepayment != null) {
                dto.setPrepaymentNo(prepayment.getPrepaymentNo());
            }
        }
        if (usage.getFeeId() != null) {
            Fee fee = feeRepository.findById(usage.getFeeId());
            if (fee != null) {
                dto.setFeeNo(fee.getFeeNo());
                dto.setFeeName(fee.getFeeName());
            }
        }
        if (usage.getMatterId() != null) {
            Matter matter = matterRepository.findById(usage.getMatterId());
            if (matter != null) {
                dto.setMatterNo(matter.getMatterNo());
                dto.setMatterName(matter.getName());
            }
        }
        if (usage.getOperatorId() != null) {
            User user = userRepository.findById(usage.getOperatorId());
            if (user != null) {
                dto.setOperatorName(user.getRealName());
            }
        }

        return dto;
    }
}
