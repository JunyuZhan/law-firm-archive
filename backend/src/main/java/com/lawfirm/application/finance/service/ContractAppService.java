package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateContractCommand;
import com.lawfirm.application.finance.command.UpdateContractCommand;
import com.lawfirm.application.finance.command.ContractChangeCommand;
import com.lawfirm.application.finance.command.CreatePaymentScheduleCommand;
import com.lawfirm.application.finance.command.UpdatePaymentScheduleCommand;
import com.lawfirm.application.finance.command.CreateParticipantCommand;
import com.lawfirm.application.finance.command.UpdateParticipantCommand;
import com.lawfirm.application.finance.dto.ContractDTO;
import com.lawfirm.application.finance.dto.ContractQueryDTO;
import com.lawfirm.application.finance.dto.ContractPaymentScheduleDTO;
import com.lawfirm.application.finance.dto.ContractParticipantDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.contract.entity.ContractTemplate;
import com.lawfirm.domain.contract.repository.ContractTemplateRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractPaymentSchedule;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.application.workbench.service.ApprovalAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.application.workbench.dto.ApprovalDTO;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.ContractPaymentScheduleRepository;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.entity.Department;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.infrastructure.persistence.mapper.ContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final ApprovalAppService approvalAppService;
    private final ApproverService approverService;
    private final ContractPaymentScheduleRepository paymentScheduleRepository;
    private final ContractParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserMapper userMapper;
    private final ContractTemplateRepository contractTemplateRepository;

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
        // 新增筛选条件
        if (StringUtils.hasText(query.getFeeType())) {
            wrapper.eq(Contract::getFeeType, query.getFeeType());
        }
        if (query.getDepartmentId() != null) {
            wrapper.eq(Contract::getDepartmentId, query.getDepartmentId());
        }
        if (query.getEffectiveDateFrom() != null) {
            wrapper.ge(Contract::getEffectiveDate, query.getEffectiveDateFrom());
        }
        if (query.getEffectiveDateTo() != null) {
            wrapper.le(Contract::getEffectiveDate, query.getEffectiveDateTo());
        }
        if (query.getExpiryDateFrom() != null) {
            wrapper.ge(Contract::getExpiryDate, query.getExpiryDateFrom());
        }
        if (query.getExpiryDateTo() != null) {
            wrapper.le(Contract::getExpiryDate, query.getExpiryDateTo());
        }
        if (query.getAmountMin() != null) {
            wrapper.ge(Contract::getTotalAmount, query.getAmountMin());
        }
        if (query.getAmountMax() != null) {
            wrapper.le(Contract::getTotalAmount, query.getAmountMax());
        }
        if (query.getClaimAmountMin() != null) {
            wrapper.ge(Contract::getClaimAmount, query.getClaimAmountMin());
        }
        if (query.getClaimAmountMax() != null) {
            wrapper.le(Contract::getClaimAmount, query.getClaimAmountMax());
        }
        if (StringUtils.hasText(query.getTrialStage())) {
            wrapper.eq(Contract::getTrialStage, query.getTrialStage());
        }
        if (StringUtils.hasText(query.getConflictCheckStatus())) {
            wrapper.eq(Contract::getConflictCheckStatus, query.getConflictCheckStatus());
        }
        if (StringUtils.hasText(query.getArchiveStatus())) {
            wrapper.eq(Contract::getArchiveStatus, query.getArchiveStatus());
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
        
        // 3. 验证风险代理比例范围
        if (command.getRiskRatio() != null) {
            if (command.getRiskRatio().compareTo(BigDecimal.ZERO) < 0 || 
                command.getRiskRatio().compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("风险代理比例必须在0-100之间");
            }
        }

        // 4. 生成合同编号
        String contractNo = generateContractNo();

        // 5. 创建合同实体
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
                // 扩展字段
                .trialStage(command.getTrialStage())
                .claimAmount(command.getClaimAmount())
                .jurisdictionCourt(command.getJurisdictionCourt())
                .opposingParty(command.getOpposingParty())
                .conflictCheckStatus(command.getConflictCheckStatus() != null ? command.getConflictCheckStatus() : "NOT_REQUIRED")
                .archiveStatus("NOT_ARCHIVED")
                .caseType(command.getCaseType())
                .causeOfAction(command.getCauseOfAction())
                .advanceTravelFee(command.getAdvanceTravelFee())
                .riskRatio(command.getRiskRatio())
                .build();

        // 6. 保存合同
        contractRepository.save(contract);

        log.info("合同创建成功: {} ({})", contract.getName(), contract.getContractNo());
        return toDTO(contract);
    }

    /**
     * 更新合同
     * 只有未审批通过的合同（DRAFT、REJECTED）可以直接修改
     * 已审批通过的合同（ACTIVE）需要通过变更审批流程
     */
    @Transactional
    public ContractDTO updateContract(UpdateContractCommand command) {
        Contract contract = contractRepository.getByIdOrThrow(command.getId(), "合同不存在");

        // 只有草稿和被拒绝状态可以修改，待审批状态不允许修改（需要先取消审批）
        if (!"DRAFT".equals(contract.getStatus()) && !"REJECTED".equals(contract.getStatus())) {
            throw new BusinessException("只有草稿状态或被拒绝状态的合同可以直接修改。已审批通过的合同如需修改，请使用变更申请功能");
        }
        
        // 验证风险代理比例范围
        if (command.getRiskRatio() != null) {
            if (command.getRiskRatio().compareTo(BigDecimal.ZERO) < 0 || 
                command.getRiskRatio().compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("风险代理比例必须在0-100之间");
            }
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
        // 扩展字段
        if (command.getCaseType() != null) {
            contract.setCaseType(command.getCaseType());
        }
        if (command.getCauseOfAction() != null) {
            contract.setCauseOfAction(command.getCauseOfAction());
        }
        if (command.getTrialStage() != null) {
            contract.setTrialStage(command.getTrialStage());
        }
        if (command.getClaimAmount() != null) {
            contract.setClaimAmount(command.getClaimAmount());
        }
        if (command.getJurisdictionCourt() != null) {
            contract.setJurisdictionCourt(command.getJurisdictionCourt());
        }
        if (command.getOpposingParty() != null) {
            contract.setOpposingParty(command.getOpposingParty());
        }
        if (command.getConflictCheckStatus() != null) {
            contract.setConflictCheckStatus(command.getConflictCheckStatus());
        }
        if (command.getArchiveStatus() != null) {
            contract.setArchiveStatus(command.getArchiveStatus());
        }
        if (command.getAdvanceTravelFee() != null) {
            contract.setAdvanceTravelFee(command.getAdvanceTravelFee());
        }
        if (command.getRiskRatio() != null) {
            contract.setRiskRatio(command.getRiskRatio());
        }
        if (command.getSealRecord() != null) {
            contract.setSealRecord(command.getSealRecord());
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
        ContractDTO dto = toDTO(contract);
        
        // 查询关联的审批单
        List<ApprovalDTO> approvals = approvalAppService.getBusinessApprovals("CONTRACT", id);
        dto.setApprovals(approvals);
        
        // 设置当前待审批的审批单
        ApprovalDTO currentApproval = approvals.stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .findFirst()
                .orElse(null);
        dto.setCurrentApproval(currentApproval);
        
        return dto;
    }

    /**
     * 提交审批（支持手动选择审批人）
     */
    @Transactional
    public void submitForApproval(Long id, Long approverId) {
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        // 允许草稿状态和被拒绝状态的合同提交审批
        if (!"DRAFT".equals(contract.getStatus()) && !"REJECTED".equals(contract.getStatus())) {
            throw new BusinessException("只有草稿状态或被拒绝状态的合同可以提交审批");
        }

        contract.setStatus("PENDING");
        contractRepository.updateById(contract);
        
        // 创建审批记录
        try {
            // 如果没有指定审批人，自动查找
            if (approverId == null) {
                approverId = approverService.findContractApprover(contract.getTotalAmount());
                if (approverId == null) {
                    log.warn("未找到合同审批人，使用默认审批人");
                    approverId = approverService.findDefaultApprover();
                }
            }
            
            if (approverId == null) {
                throw new BusinessException("无法找到审批人，请选择审批人或配置系统审批人");
            }
            
            // 验证审批人存在
            User approver = userRepository.getById(approverId);
            if (approver == null) {
                throw new BusinessException("选择的审批人不存在");
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
            
            log.info("合同提交审批成功: {} (审批人: {})", contract.getName(), approver.getRealName());
        } catch (BusinessException e) {
            // 回滚合同状态
            contract.setStatus("DRAFT");
            contractRepository.updateById(contract);
            log.error("合同提交审批失败: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // 回滚合同状态
            contract.setStatus("DRAFT");
            contractRepository.updateById(contract);
            log.error("合同提交审批异常: {}", e.getMessage(), e);
            throw new BusinessException("提交审批失败: " + e.getMessage());
        }
    }

    /**
     * 获取可选审批人列表（当前用户架构垂直线上的领导）
     * 包括：当前部门负责人、上级部门负责人、主任、合伙人
     */
    public List<Map<String, Object>> getAvailableApprovers() {
        Long currentUserId = SecurityUtils.getUserId();
        Long currentDeptId = SecurityUtils.getDepartmentId();
        
        List<Map<String, Object>> approvers = new java.util.ArrayList<>();
        java.util.Set<Long> addedUserIds = new java.util.HashSet<>();
        
        // 1. 沿部门层级向上查找所有部门负责人
        Long deptId = currentDeptId;
        while (deptId != null) {
            Department dept = departmentRepository.getById(deptId);
            if (dept != null && dept.getLeaderId() != null && !dept.getLeaderId().equals(currentUserId)) {
                if (!addedUserIds.contains(dept.getLeaderId())) {
                    User leader = userRepository.getById(dept.getLeaderId());
                    if (leader != null && "ACTIVE".equals(leader.getStatus())) {
                        Map<String, Object> approver = new HashMap<>();
                        approver.put("id", leader.getId());
                        approver.put("realName", leader.getRealName());
                        approver.put("departmentName", dept.getName());
                        approver.put("position", "部门负责人");
                        approvers.add(approver);
                        addedUserIds.add(leader.getId());
                    }
                }
                deptId = dept.getParentId();
            } else if (dept != null) {
                deptId = dept.getParentId();
            } else {
                deptId = null;
            }
        }
        
        // 2. 添加所有合伙人（PARTNER角色）
        List<Long> partnerIds = userMapper.selectUserIdsByRoleCode("PARTNER");
        if (partnerIds != null) {
            for (Long partnerId : partnerIds) {
                if (!partnerId.equals(currentUserId) && !addedUserIds.contains(partnerId)) {
                    User partner = userRepository.getById(partnerId);
                    if (partner != null && "ACTIVE".equals(partner.getStatus())) {
                        Map<String, Object> approver = new HashMap<>();
                        approver.put("id", partner.getId());
                        approver.put("realName", partner.getRealName());
                        approver.put("departmentName", getDepartmentName(partner.getDepartmentId()));
                        approver.put("position", "合伙人");
                        approvers.add(approver);
                        addedUserIds.add(partner.getId());
                    }
                }
            }
        }
        
        // 3. 添加主任（DIRECTOR角色）
        List<Long> directorIds = userMapper.selectUserIdsByRoleCode("DIRECTOR");
        if (directorIds != null) {
            for (Long directorId : directorIds) {
                if (!directorId.equals(currentUserId) && !addedUserIds.contains(directorId)) {
                    User director = userRepository.getById(directorId);
                    if (director != null && "ACTIVE".equals(director.getStatus())) {
                        Map<String, Object> approver = new HashMap<>();
                        approver.put("id", director.getId());
                        approver.put("realName", director.getRealName());
                        approver.put("departmentName", getDepartmentName(director.getDepartmentId()));
                        approver.put("position", "主任");
                        approvers.add(approver);
                        addedUserIds.add(director.getId());
                    }
                }
            }
        }
        
        return approvers;
    }
    
    private String getDepartmentName(Long deptId) {
        if (deptId == null) return "";
        Department dept = departmentRepository.getById(deptId);
        return dept != null ? dept.getName() : "";
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
     * 获取已审批的合同列表（用于创建项目时选择）
     * 包含参与人信息，便于创建项目时自动填充
     */
    public List<ContractDTO> getApprovedContracts() {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getStatus, "ACTIVE");
        // 只返回未关联项目的合同
        wrapper.isNull(Contract::getMatterId);
        wrapper.orderByDesc(Contract::getCreatedAt);
        
        List<Contract> contracts = contractRepository.list(wrapper);
        return contracts.stream()
                .map(contract -> {
                    ContractDTO dto = toDTO(contract);
                    // 加载参与人信息
                    List<ContractParticipant> participants = participantRepository.findByContractId(contract.getId());
                    if (participants != null && !participants.isEmpty()) {
                        dto.setParticipants(participants.stream()
                                .map(this::toParticipantDTO)
                                .collect(Collectors.toList()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 申请合同变更
     * 用于已审批通过的合同申请变更，需要重新审批
     */
    @Transactional
    public void applyContractChange(ContractChangeCommand command) {
        Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");
        
        // 只有已审批通过的合同可以申请变更
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new BusinessException("只有已审批通过的合同可以申请变更");
        }
        
        // 构建变更内容说明（记录所有变更的字段）
        StringBuilder changeDesc = new StringBuilder();
        changeDesc.append("变更原因：").append(command.getChangeReason()).append("\n");
        changeDesc.append("变更内容：\n");
        
        if (command.getName() != null && !command.getName().equals(contract.getName())) {
            changeDesc.append("- 合同名称：").append(contract.getName()).append(" → ").append(command.getName()).append("\n");
        }
        if (command.getContractType() != null && !command.getContractType().equals(contract.getContractType())) {
            changeDesc.append("- 合同类型：").append(contract.getContractType()).append(" → ").append(command.getContractType()).append("\n");
        }
        if (command.getClientId() != null && !command.getClientId().equals(contract.getClientId())) {
            changeDesc.append("- 客户变更\n");
        }
        if (command.getFeeType() != null && !command.getFeeType().equals(contract.getFeeType())) {
            changeDesc.append("- 收费方式：").append(contract.getFeeType()).append(" → ").append(command.getFeeType()).append("\n");
        }
        if (command.getTotalAmount() != null && command.getTotalAmount().compareTo(contract.getTotalAmount()) != 0) {
            changeDesc.append("- 合同金额：").append(contract.getTotalAmount()).append(" → ").append(command.getTotalAmount()).append("\n");
        }
        if (command.getSignDate() != null && !command.getSignDate().equals(contract.getSignDate())) {
            changeDesc.append("- 签约日期变更\n");
        }
        if (command.getEffectiveDate() != null && !command.getEffectiveDate().equals(contract.getEffectiveDate())) {
            changeDesc.append("- 生效日期变更\n");
        }
        if (command.getExpiryDate() != null && !command.getExpiryDate().equals(contract.getExpiryDate())) {
            changeDesc.append("- 到期日期变更\n");
        }
        if (command.getPaymentTerms() != null && !command.getPaymentTerms().equals(contract.getPaymentTerms())) {
            changeDesc.append("- 付款条款变更\n");
        }
        
        if (command.getChangeDescription() != null && !command.getChangeDescription().trim().isEmpty()) {
            changeDesc.append("\n详细说明：").append(command.getChangeDescription());
        }
        
        // 将变更内容保存到备注中（作为变更记录）
        String changeRecord = String.format("[变更申请] %s\n%s", 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                changeDesc.toString());
        String newRemark = contract.getRemark() != null 
                ? contract.getRemark() + "\n\n" + changeRecord
                : changeRecord;
        
        // 更新合同为待审批状态，并保存变更内容到备注
        contract.setStatus("PENDING");
        contract.setRemark(newRemark);
        
        // 应用变更内容（但状态为待审批，需要审批通过后才生效）
        if (command.getName() != null) {
            contract.setName(command.getName());
        }
        if (command.getContractType() != null) {
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
        if (command.getFeeType() != null) {
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
        
        contractRepository.updateById(contract);
        
        // 创建变更审批记录
        try {
            Long approverId = approverService.findContractApprover(contract.getTotalAmount());
            if (approverId == null) {
                log.warn("未找到合同审批人，使用默认审批人");
                approverId = approverService.findDefaultApprover();
            }
            
            if (approverId == null) {
                throw new BusinessException("无法找到审批人，请先配置系统审批人");
            }
            
            String priority = contract.getTotalAmount() != null && 
                              contract.getTotalAmount().compareTo(new BigDecimal("100000")) >= 0 ? "HIGH" : "MEDIUM";
            
            // 创建变更审批，业务标题包含"变更"标识
            approvalService.createApproval(
                    "CONTRACT",
                    contract.getId(),
                    contract.getContractNo(),
                    contract.getName() + " [变更申请]",
                    approverId,
                    priority,
                    "NORMAL",
                    changeDesc.toString()  // 将变更内容作为业务快照
            );
            
            log.info("合同变更申请提交成功: {} (审批人: {})", contract.getName(), approverId);
        } catch (BusinessException e) {
            // 回滚合同状态
            contract.setStatus("ACTIVE");
            contractRepository.updateById(contract);
            log.error("合同变更申请提交失败: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // 回滚合同状态
            contract.setStatus("ACTIVE");
            contractRepository.updateById(contract);
            log.error("合同变更申请提交异常: {}", e.getMessage(), e);
            throw new BusinessException("提交变更申请失败: " + e.getMessage());
        }
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
        // 填充客户名称
        if (contract.getClientId() != null) {
            try {
                var client = clientRepository.getById(contract.getClientId());
                if (client != null) {
                    dto.setClientName(client.getName());
                }
            } catch (Exception e) {
                log.warn("获取客户名称失败，clientId: {}", contract.getClientId(), e);
            }
        }
        dto.setMatterId(contract.getMatterId());
        // 填充项目名称
        if (contract.getMatterId() != null) {
            try {
                var matter = matterRepository.getById(contract.getMatterId());
                if (matter != null) {
                    dto.setMatterName(matter.getName());
                }
            } catch (Exception e) {
                log.warn("获取项目名称失败，matterId: {}", contract.getMatterId(), e);
            }
        }
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
        // 扩展字段
        dto.setCaseType(contract.getCaseType());
        dto.setCaseTypeName(getCaseTypeName(contract.getCaseType()));
        dto.setCauseOfAction(contract.getCauseOfAction());
        // 案由名称由前端根据code查找，后端只存储code
        dto.setTrialStage(contract.getTrialStage());
        dto.setTrialStageName(getTrialStageName(contract.getTrialStage()));
        dto.setClaimAmount(contract.getClaimAmount());
        dto.setJurisdictionCourt(contract.getJurisdictionCourt());
        dto.setOpposingParty(contract.getOpposingParty());
        dto.setConflictCheckStatus(contract.getConflictCheckStatus());
        dto.setConflictCheckStatusName(getConflictCheckStatusName(contract.getConflictCheckStatus()));
        dto.setArchiveStatus(contract.getArchiveStatus());
        dto.setArchiveStatusName(getArchiveStatusName(contract.getArchiveStatus()));
        dto.setAdvanceTravelFee(contract.getAdvanceTravelFee());
        dto.setRiskRatio(contract.getRiskRatio());
        dto.setSealRecord(contract.getSealRecord());
        return dto;
    }
    
    /**
     * 获取案件类型名称
     */
    private String getCaseTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "CIVIL" -> "民事案件";
            case "CRIMINAL" -> "刑事案件";
            case "ADMINISTRATIVE" -> "行政案件";
            case "BANKRUPTCY" -> "破产案件";
            case "IP" -> "知识产权案件";
            case "ARBITRATION" -> "仲裁案件";
            case "ENFORCEMENT" -> "执行案件";
            case "LEGAL_COUNSEL" -> "法律顾问";
            case "SPECIAL_SERVICE" -> "专项服务";
            default -> type;
        };
    }
    
    /**
     * 获取审理阶段名称
     */
    private String getTrialStageName(String stage) {
        if (stage == null) return null;
        return switch (stage) {
            case "FIRST_INSTANCE" -> "一审";
            case "SECOND_INSTANCE" -> "二审";
            case "RETRIAL" -> "再审";
            case "EXECUTION" -> "执行";
            case "NON_LITIGATION" -> "非诉";
            default -> stage;
        };
    }
    
    /**
     * 获取利冲审查状态名称
     */
    private String getConflictCheckStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待审查";
            case "PASSED" -> "已通过";
            case "FAILED" -> "未通过";
            case "NOT_REQUIRED" -> "无需审查";
            default -> status;
        };
    }
    
    /**
     * 获取归档状态名称
     */
    private String getArchiveStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "NOT_ARCHIVED" -> "未归档";
            case "ARCHIVED" -> "已归档";
            case "DESTROYED" -> "已销毁";
            default -> status;
        };
    }
    
    /**
     * 获取付款计划状态名称
     */
    private String getPaymentScheduleStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待收";
            case "PARTIAL" -> "部分收款";
            case "PAID" -> "已收清";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
    
    /**
     * 获取参与人角色名称
     */
    private String getParticipantRoleName(String role) {
        if (role == null) return null;
        return switch (role) {
            case "LEAD" -> "承办律师";
            case "CO_COUNSEL" -> "协办律师";
            case "ORIGINATOR" -> "案源人";
            case "PARALEGAL" -> "律师助理";
            default -> role;
        };
    }
    
    // ========== 付款计划管理 ==========
    
    /**
     * 获取合同的付款计划列表
     */
    public List<ContractPaymentScheduleDTO> getPaymentSchedules(Long contractId) {
        contractRepository.getByIdOrThrow(contractId, "合同不存在");
        List<ContractPaymentSchedule> schedules = paymentScheduleRepository.findByContractId(contractId);
        return schedules.stream().map(this::toPaymentScheduleDTO).collect(Collectors.toList());
    }
    
    /**
     * 创建付款计划
     */
    @Transactional
    public ContractPaymentScheduleDTO createPaymentSchedule(CreatePaymentScheduleCommand command) {
        Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");
        
        ContractPaymentSchedule schedule = ContractPaymentSchedule.builder()
                .contractId(command.getContractId())
                .phaseName(command.getPhaseName())
                .amount(command.getAmount())
                .percentage(command.getPercentage())
                .plannedDate(command.getPlannedDate())
                .status("PENDING")
                .remark(command.getRemark())
                .build();
        
        paymentScheduleRepository.save(schedule);
        log.info("付款计划创建成功: {} - {}", contract.getContractNo(), command.getPhaseName());
        return toPaymentScheduleDTO(schedule);
    }
    
    /**
     * 更新付款计划
     */
    @Transactional
    public ContractPaymentScheduleDTO updatePaymentSchedule(UpdatePaymentScheduleCommand command) {
        ContractPaymentSchedule schedule = paymentScheduleRepository.getByIdOrThrow(command.getId(), "付款计划不存在");
        
        if (command.getPhaseName() != null) {
            schedule.setPhaseName(command.getPhaseName());
        }
        if (command.getAmount() != null) {
            schedule.setAmount(command.getAmount());
        }
        if (command.getPercentage() != null) {
            schedule.setPercentage(command.getPercentage());
        }
        if (command.getPlannedDate() != null) {
            schedule.setPlannedDate(command.getPlannedDate());
        }
        if (command.getActualDate() != null) {
            schedule.setActualDate(command.getActualDate());
        }
        if (command.getStatus() != null) {
            schedule.setStatus(command.getStatus());
        }
        if (command.getRemark() != null) {
            schedule.setRemark(command.getRemark());
        }
        
        paymentScheduleRepository.updateById(schedule);
        log.info("付款计划更新成功: {}", schedule.getId());
        return toPaymentScheduleDTO(schedule);
    }
    
    /**
     * 删除付款计划
     */
    @Transactional
    public void deletePaymentSchedule(Long id) {
        ContractPaymentSchedule schedule = paymentScheduleRepository.getByIdOrThrow(id, "付款计划不存在");
        paymentScheduleRepository.removeById(id);
        log.info("付款计划删除成功: {}", id);
    }
    
    /**
     * 付款计划 Entity 转 DTO
     */
    private ContractPaymentScheduleDTO toPaymentScheduleDTO(ContractPaymentSchedule schedule) {
        ContractPaymentScheduleDTO dto = new ContractPaymentScheduleDTO();
        dto.setId(schedule.getId());
        dto.setContractId(schedule.getContractId());
        dto.setPhaseName(schedule.getPhaseName());
        dto.setAmount(schedule.getAmount());
        dto.setPercentage(schedule.getPercentage());
        dto.setPlannedDate(schedule.getPlannedDate());
        dto.setActualDate(schedule.getActualDate());
        dto.setStatus(schedule.getStatus());
        dto.setStatusName(getPaymentScheduleStatusName(schedule.getStatus()));
        dto.setRemark(schedule.getRemark());
        dto.setCreatedAt(schedule.getCreatedAt());
        dto.setUpdatedAt(schedule.getUpdatedAt());
        return dto;
    }
    
    // ========== 参与人管理 ==========
    
    /**
     * 获取合同的参与人列表
     */
    public List<ContractParticipantDTO> getParticipants(Long contractId) {
        contractRepository.getByIdOrThrow(contractId, "合同不存在");
        List<ContractParticipant> participants = participantRepository.findByContractId(contractId);
        return participants.stream().map(this::toParticipantDTO).collect(Collectors.toList());
    }
    
    /**
     * 创建参与人
     */
    @Transactional
    public ContractParticipantDTO createParticipant(CreateParticipantCommand command) {
        Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");
        
        // 检查用户是否已是参与人
        if (participantRepository.existsByContractIdAndUserId(command.getContractId(), command.getUserId())) {
            throw new BusinessException("该用户已是合同参与人");
        }
        
        // 验证提成比例
        if (command.getCommissionRate() != null) {
            BigDecimal currentTotal = participantRepository.sumCommissionRateByContractId(command.getContractId());
            BigDecimal newTotal = currentTotal.add(command.getCommissionRate());
            if (newTotal.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("提成比例总和不能超过100%，当前已分配: " + currentTotal + "%");
            }
        }
        
        ContractParticipant participant = ContractParticipant.builder()
                .contractId(command.getContractId())
                .userId(command.getUserId())
                .role(command.getRole())
                .commissionRate(command.getCommissionRate())
                .remark(command.getRemark())
                .build();
        
        participantRepository.save(participant);
        log.info("合同参与人创建成功: {} - userId: {}", contract.getContractNo(), command.getUserId());
        return toParticipantDTO(participant);
    }
    
    /**
     * 更新参与人
     */
    @Transactional
    public ContractParticipantDTO updateParticipant(UpdateParticipantCommand command) {
        ContractParticipant participant = participantRepository.getByIdOrThrow(command.getId(), "参与人不存在");
        
        // 验证提成比例
        if (command.getCommissionRate() != null) {
            BigDecimal currentTotal = participantRepository.sumCommissionRateByContractId(participant.getContractId());
            BigDecimal oldRate = participant.getCommissionRate() != null ? participant.getCommissionRate() : BigDecimal.ZERO;
            BigDecimal newTotal = currentTotal.subtract(oldRate).add(command.getCommissionRate());
            if (newTotal.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("提成比例总和不能超过100%");
            }
        }
        
        if (command.getRole() != null) {
            participant.setRole(command.getRole());
        }
        if (command.getCommissionRate() != null) {
            participant.setCommissionRate(command.getCommissionRate());
        }
        if (command.getRemark() != null) {
            participant.setRemark(command.getRemark());
        }
        
        participantRepository.updateById(participant);
        log.info("合同参与人更新成功: {}", participant.getId());
        return toParticipantDTO(participant);
    }
    
    /**
     * 删除参与人
     */
    @Transactional
    public void deleteParticipant(Long id) {
        ContractParticipant participant = participantRepository.getByIdOrThrow(id, "参与人不存在");
        
        // 检查是否是唯一的承办律师
        if ("LEAD".equals(participant.getRole())) {
            List<ContractParticipant> leads = participantRepository.findByContractId(participant.getContractId())
                    .stream()
                    .filter(p -> "LEAD".equals(p.getRole()) && !p.getId().equals(id))
                    .collect(Collectors.toList());
            if (leads.isEmpty()) {
                throw new BusinessException("合同必须至少有一个承办律师");
            }
        }
        
        participantRepository.removeById(id);
        log.info("合同参与人删除成功: {}", id);
    }
    
    /**
     * 验证合同是否有承办律师
     */
    public boolean hasLeadParticipant(Long contractId) {
        ContractParticipant lead = participantRepository.findLeadByContractId(contractId);
        return lead != null;
    }
    
    /**
     * 参与人 Entity 转 DTO
     */
    private ContractParticipantDTO toParticipantDTO(ContractParticipant participant) {
        ContractParticipantDTO dto = new ContractParticipantDTO();
        dto.setId(participant.getId());
        dto.setContractId(participant.getContractId());
        dto.setUserId(participant.getUserId());
        // 填充用户名称
        if (participant.getUserId() != null) {
            try {
                var user = userRepository.getById(participant.getUserId());
                if (user != null) {
                    dto.setUserName(user.getRealName());
                }
            } catch (Exception e) {
                log.warn("获取用户名称失败，userId: {}", participant.getUserId(), e);
            }
        }
        dto.setRole(participant.getRole());
        dto.setRoleName(getParticipantRoleName(participant.getRole()));
        dto.setCommissionRate(participant.getCommissionRate());
        dto.setRemark(participant.getRemark());
        dto.setCreatedAt(participant.getCreatedAt());
        dto.setUpdatedAt(participant.getUpdatedAt());
        return dto;
    }
    
    // ========== 合同模板功能 ==========
    
    /**
     * 基于模板创建合同
     * 选择模板时自动填充 contract_type, fee_type, content 字段
     */
    @Transactional
    public ContractDTO createFromTemplate(Long templateId, CreateContractCommand command) {
        // 获取模板
        ContractTemplate template = contractTemplateRepository.getByIdOrThrow(templateId, "模板不存在");
        
        if (!"ACTIVE".equals(template.getStatus())) {
            throw new BusinessException("模板已停用，无法使用");
        }
        
        // 从模板填充字段（如果命令中未指定）
        if (!StringUtils.hasText(command.getContractType())) {
            command.setContractType(template.getContractType());
        }
        if (!StringUtils.hasText(command.getFeeType())) {
            command.setFeeType(template.getFeeType());
        }
        
        // 创建合同
        ContractDTO contract = createContract(command);
        
        // 处理模板内容的变量替换
        if (StringUtils.hasText(template.getContent())) {
            String processedContent = processTemplateVariables(template.getContent(), contract, command);
            // 更新合同的备注或内容字段（这里假设用 remark 存储合同内容）
            Contract entity = contractRepository.getById(contract.getId());
            entity.setRemark(processedContent);
            contractRepository.updateById(entity);
            contract.setRemark(processedContent);
        }
        
        log.info("基于模板创建合同成功: templateId={}, contractId={}", templateId, contract.getId());
        return contract;
    }
    
    /**
     * 处理模板变量替换
     * 支持的变量：${clientName}, ${totalAmount}, ${signDate}, ${lawyerName}, ${contractNo}, ${effectiveDate}, ${expiryDate}
     */
    public String processTemplateVariables(String templateContent, ContractDTO contract, CreateContractCommand command) {
        if (!StringUtils.hasText(templateContent)) {
            return templateContent;
        }
        
        Map<String, String> variables = new HashMap<>();
        
        // 客户名称
        if (contract.getClientId() != null) {
            try {
                Client client = clientRepository.getById(contract.getClientId());
                if (client != null) {
                    variables.put("clientName", client.getName());
                }
            } catch (Exception e) {
                log.warn("获取客户名称失败", e);
            }
        }
        
        // 合同金额
        if (contract.getTotalAmount() != null) {
            variables.put("totalAmount", contract.getTotalAmount().toString());
        }
        
        // 签约日期
        if (contract.getSignDate() != null) {
            variables.put("signDate", contract.getSignDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        }
        
        // 生效日期
        if (contract.getEffectiveDate() != null) {
            variables.put("effectiveDate", contract.getEffectiveDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        }
        
        // 到期日期
        if (contract.getExpiryDate() != null) {
            variables.put("expiryDate", contract.getExpiryDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        }
        
        // 合同编号
        if (contract.getContractNo() != null) {
            variables.put("contractNo", contract.getContractNo());
        }
        
        // 律师名称（签约人）
        if (contract.getSignerId() != null) {
            try {
                User user = userRepository.getById(contract.getSignerId());
                if (user != null) {
                    variables.put("lawyerName", user.getRealName());
                }
            } catch (Exception e) {
                log.warn("获取律师名称失败", e);
            }
        }
        
        // 标的金额
        if (contract.getClaimAmount() != null) {
            variables.put("claimAmount", contract.getClaimAmount().toString());
        }
        
        // 管辖法院
        if (contract.getJurisdictionCourt() != null) {
            variables.put("jurisdictionCourt", contract.getJurisdictionCourt());
        }
        
        // 对方当事人
        if (contract.getOpposingParty() != null) {
            variables.put("opposingParty", contract.getOpposingParty());
        }
        
        // 执行变量替换
        return replaceVariables(templateContent, variables);
    }
    
    /**
     * 替换模板中的变量
     * 变量格式：${variableName}
     */
    private String replaceVariables(String content, Map<String, String> variables) {
        if (content == null || variables == null || variables.isEmpty()) {
            return content;
        }
        
        String result = content;
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(content);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            String replacement = variables.getOrDefault(variableName, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * 预览模板变量替换结果
     * 用于前端预览合同内容
     */
    public String previewTemplateContent(Long templateId, CreateContractCommand command) {
        ContractTemplate template = contractTemplateRepository.getByIdOrThrow(templateId, "模板不存在");
        
        if (!StringUtils.hasText(template.getContent())) {
            return "";
        }
        
        // 构建临时 DTO 用于变量替换
        ContractDTO tempDto = new ContractDTO();
        tempDto.setClientId(command.getClientId());
        tempDto.setTotalAmount(command.getTotalAmount());
        tempDto.setSignDate(command.getSignDate());
        tempDto.setEffectiveDate(command.getEffectiveDate());
        tempDto.setExpiryDate(command.getExpiryDate());
        tempDto.setSignerId(command.getSignerId());
        tempDto.setClaimAmount(command.getClaimAmount());
        tempDto.setJurisdictionCourt(command.getJurisdictionCourt());
        tempDto.setOpposingParty(command.getOpposingParty());
        tempDto.setContractNo("[待生成]");
        
        return processTemplateVariables(template.getContent(), tempDto, command);
    }
}

