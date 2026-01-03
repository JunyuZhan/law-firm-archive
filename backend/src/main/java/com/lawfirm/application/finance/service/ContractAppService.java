package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.command.UpdateContractCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 合同应用服务
 */
@Slf4j
@Service("financeContractAppService")
@RequiredArgsConstructor
public class ContractAppService {

    private final ContractRepository contractRepository;
    private final ContractMapper contractMapper;
    private final ClientRepository clientRepository;
    private final MatterRepository matterRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;

    /**
     * 分页查询合同
     */
    public PageResult<ContractDTO> listContracts(ContractQueryDTO query) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getContractNo())) {
            wrapper.like(Contract::getContractNo, query.getContractNo());
        }
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(Contract::getName, query.getName());
        }
        if (query.getClientId() != null) {
            wrapper.eq(Contract::getClientId, query.getClientId());
        }
        if (query.getMatterId() != null) {
            wrapper.eq(Contract::getMatterId, query.getMatterId());
        }
        if (StringUtils.hasText(query.getContractType())) {
            wrapper.eq(Contract::getContractType, query.getContractType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Contract::getStatus, query.getStatus());
        }
        if (query.getSignDateFrom() != null) {
            wrapper.ge(Contract::getSignDate, query.getSignDateFrom());
        }
        if (query.getSignDateTo() != null) {
            wrapper.le(Contract::getSignDate, query.getSignDateTo());
        }
        if (query.getSignerId() != null) {
            wrapper.eq(Contract::getSignerId, query.getSignerId());
        }
        
        wrapper.orderByDesc(Contract::getCreatedAt);

        IPage<Contract> page = contractRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<ContractDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建合同
     */
    @Transactional
    public ContractDTO createContract(CreateContractCommand command) {
        // 1. 验证客户存在
        clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 2. 验证案件存在（如果指定）
        if (command.getMatterId() != null) {
            matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
        }

        // 3. 生成合同编号
        String contractNo = generateContractNo();

        // 4. 创建合同实体
        Contract contract = Contract.builder()
                .contractNo(contractNo)
                .name(command.getName())
                .contractType(command.getContractType())
                .clientId(command.getClientId())
                .matterId(command.getMatterId())
                .feeType(command.getFeeType())
                .totalAmount(command.getTotalAmount())
                .paidAmount(BigDecimal.ZERO)
                .currency(command.getCurrency() != null ? command.getCurrency() : "CNY")
                .signDate(command.getSignDate())
                .effectiveDate(command.getEffectiveDate())
                .expiryDate(command.getExpiryDate())
                .status("DRAFT")
                .signerId(command.getSignerId() != null ? command.getSignerId() : SecurityUtils.getUserId())
                .departmentId(command.getDepartmentId() != null ? command.getDepartmentId() : SecurityUtils.getDepartmentId())
                .paymentTerms(command.getPaymentTerms())
                .fileUrl(command.getFileUrl())
                .remark(command.getRemark())
                .build();

        // 5. 保存合同
        contractRepository.save(contract);

        log.info("合同创建成功: {} ({})", contract.getName(), contract.getContractNo());
        return toDTO(contract);
    }

    /**
     * 更新合同
     */
    @Transactional
    public ContractDTO updateContract(UpdateContractCommand command) {
        Contract contract = contractRepository.getByIdOrThrow(command.getId(), "合同不存在");

        // 只有草稿和待审批状态可以修改
        if (!"DRAFT".equals(contract.getStatus()) && !"PENDING".equals(contract.getStatus())) {
            throw new BusinessException("当前状态不允许修改");
        }

        // 更新字段
        if (StringUtils.hasText(command.getName())) {
            contract.setName(command.getName());
        }
        if (StringUtils.hasText(command.getContractType())) {
            contract.setContractType(command.getContractType());
        }
        if (command.getClientId() != null) {
            clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");
            contract.setClientId(command.getClientId());
        }
        if (command.getMatterId() != null) {
            matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
            contract.setMatterId(command.getMatterId());
        }
        if (StringUtils.hasText(command.getFeeType())) {
            contract.setFeeType(command.getFeeType());
        }
        if (command.getTotalAmount() != null) {
            contract.setTotalAmount(command.getTotalAmount());
        }
        if (command.getCurrency() != null) {
            contract.setCurrency(command.getCurrency());
        }
        if (command.getSignDate() != null) {
            contract.setSignDate(command.getSignDate());
        }
        if (command.getEffectiveDate() != null) {
            contract.setEffectiveDate(command.getEffectiveDate());
        }
        if (command.getExpiryDate() != null) {
            contract.setExpiryDate(command.getExpiryDate());
        }
        if (command.getSignerId() != null) {
            contract.setSignerId(command.getSignerId());
        }
        if (command.getDepartmentId() != null) {
            contract.setDepartmentId(command.getDepartmentId());
        }
        if (command.getPaymentTerms() != null) {
            contract.setPaymentTerms(command.getPaymentTerms());
        }
        if (command.getFileUrl() != null) {
            contract.setFileUrl(command.getFileUrl());
        }
        if (command.getRemark() != null) {
            contract.setRemark(command.getRemark());
        }

        contractRepository.updateById(contract);
        log.info("合同更新成功: {}", contract.getName());
        return toDTO(contract);
    }

    /**
     * 删除合同
     */
    @Transactional
    public void deleteContract(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new BusinessException("只有草稿状态的合同可以删除");
        }

        contractMapper.deleteById(id);
        log.info("合同删除成功: {}", contract.getName());
    }

    /**
     * 获取合同详情
     */
    public ContractDTO getContractById(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        return toDTO(contract);
    }

    /**
     * 提交审批
     */
    @Transactional
    public void submitForApproval(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        if (!"DRAFT".equals(contract.getStatus())) {
            throw new BusinessException("只有草稿状态可以提交审批");
        }

        contract.setStatus("PENDING");
        contractRepository.updateById(contract);
        
        // 创建审批记录
        Long approverId = approverService.findContractApprover(contract.getTotalAmount());
        if (approverId == null) {
            approverId = approverService.findDefaultApprover();
        }
        
        String priority = contract.getTotalAmount() != null && 
                          contract.getTotalAmount().compareTo(new BigDecimal("100000")) >= 0 ? "HIGH" : "MEDIUM";
        
        approvalService.createApproval(
                "CONTRACT",
                contract.getId(),
                contract.getContractNo(),
                contract.getName(),
                approverId,
                priority,
                "NORMAL",
                null  // businessSnapshot
        );
        
        log.info("合同提交审批: {} (审批人: {})", contract.getName(), approverId);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        if (!"PENDING".equals(contract.getStatus())) {
            throw new BusinessException("只有待审批状态可以审批");
        }

        contract.setStatus("ACTIVE");
        if (contract.getEffectiveDate() == null) {
            contract.setEffectiveDate(LocalDate.now());
        }
        contractRepository.updateById(contract);
        log.info("合同审批通过: {}", contract.getName());
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(Long id, String reason) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        if (!"PENDING".equals(contract.getStatus())) {
            throw new BusinessException("只有待审批状态可以拒绝");
        }

        contract.setStatus("REJECTED");
        contract.setRemark(reason);
        contractRepository.updateById(contract);
        log.info("合同审批拒绝: {}", contract.getName());
    }

    /**
     * 终止合同
     */
    @Transactional
    public void terminate(Long id, String reason) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new BusinessException("只有生效中的合同可以终止");
        }

        contract.setStatus("TERMINATED");
        contract.setRemark(reason);
        contractRepository.updateById(contract);
        log.info("合同已终止: {}", contract.getName());
    }

    /**
     * 完成合同
     */
    @Transactional
    public void complete(Long id) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new BusinessException("只有生效中的合同可以完成");
        }

        contract.setStatus("COMPLETED");
        contractRepository.updateById(contract);
        log.info("合同已完成: {}", contract.getName());
    }

    /**
     * 生成合同编号
     */
    private String generateContractNo() {
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "HT" + datePart + random;
    }

    /**
     * 获取合同类型名称
     */
    private String getContractTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "SERVICE" -> "服务合同";
            case "RETAINER" -> "常年法顾";
            case "LITIGATION" -> "诉讼代理";
            case "NON_LITIGATION" -> "非诉项目";
            default -> type;
        };
    }

    /**
     * 获取收费方式名称
     */
    private String getFeeTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "FIXED" -> "固定收费";
            case "HOURLY" -> "计时收费";
            case "CONTINGENCY" -> "风险代理";
            case "MIXED" -> "混合收费";
            default -> type;
        };
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审批";
            case "ACTIVE" -> "生效中";
            case "REJECTED" -> "已拒绝";
            case "TERMINATED" -> "已终止";
            case "COMPLETED" -> "已完成";
            case "EXPIRED" -> "已过期";
            default -> status;
        };
    }

    /**
     * Entity 转 DTO
     */
    private ContractDTO toDTO(Contract contract) {
        ContractDTO dto = new ContractDTO();
        dto.setId(contract.getId());
        dto.setContractNo(contract.getContractNo());
        dto.setName(contract.getName());
        dto.setContractType(contract.getContractType());
        dto.setContractTypeName(getContractTypeName(contract.getContractType()));
        dto.setClientId(contract.getClientId());
        dto.setMatterId(contract.getMatterId());
        dto.setFeeType(contract.getFeeType());
        dto.setFeeTypeName(getFeeTypeName(contract.getFeeType()));
        dto.setTotalAmount(contract.getTotalAmount());
        dto.setPaidAmount(contract.getPaidAmount());
        dto.setUnpaidAmount(contract.getTotalAmount().subtract(
                contract.getPaidAmount() != null ? contract.getPaidAmount() : BigDecimal.ZERO));
        dto.setCurrency(contract.getCurrency());
        dto.setSignDate(contract.getSignDate());
        dto.setEffectiveDate(contract.getEffectiveDate());
        dto.setExpiryDate(contract.getExpiryDate());
        dto.setStatus(contract.getStatus());
        dto.setStatusName(getStatusName(contract.getStatus()));
        dto.setSignerId(contract.getSignerId());
        dto.setDepartmentId(contract.getDepartmentId());
        dto.setPaymentTerms(contract.getPaymentTerms());
        dto.setFileUrl(contract.getFileUrl());
        dto.setRemark(contract.getRemark());
        dto.setCreatedAt(contract.getCreatedAt());
        dto.setUpdatedAt(contract.getUpdatedAt());
        return dto;
    }
}

