package com.lawfirm.application.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.admin.dto.AdminContractQueryDTO;
import com.lawfirm.application.admin.dto.AdminContractViewDTO;
import com.lawfirm.application.common.service.ContractDataPermissionService;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.constant.MatterConstants;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行政模块合同查询服务（只读）
 * 用于司法局报备、介绍信等
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 8.1, 8.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContractQueryService {

    private final ContractRepository contractRepository;
    private final ContractParticipantRepository participantRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ContractDataPermissionService dataPermissionService;

    /**
     * 查询已审批合同列表（只读）
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4, 8.1
     */
    public PageResult<AdminContractViewDTO> listApprovedContracts(AdminContractQueryDTO query) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询已审批通过的合同
        wrapper.eq(Contract::getStatus, "ACTIVE");
        
        // 数据权限控制 (Requirement 8.1)
        List<Long> accessibleContractIds = dataPermissionService.getAccessibleContractIds();
        if (accessibleContractIds != null) {
            // 非管理角色，只能访问自己参与的合同
            if (accessibleContractIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(Contract::getId, accessibleContractIds);
        }
        
        // 按合同编号筛选
        if (StringUtils.hasText(query.getContractNo())) {
            wrapper.like(Contract::getContractNo, query.getContractNo());
        }
        
        // 按案件类型筛选
        if (StringUtils.hasText(query.getCaseType())) {
            wrapper.eq(Contract::getCaseType, query.getCaseType());
        }
        
        // 按签约日期范围筛选（用于按月导出）
        if (query.getSignDateFrom() != null) {
            wrapper.ge(Contract::getSignDate, query.getSignDateFrom());
        }
        if (query.getSignDateTo() != null) {
            wrapper.le(Contract::getSignDate, query.getSignDateTo());
        }
        
        // 按承办律师筛选
        if (query.getLeadLawyerId() != null) {
            // 通过参与人表关联查询
            List<Long> contractIds = participantRepository.findContractIdsByUserIdAndRole(
                query.getLeadLawyerId(), "LEAD");
            if (contractIds.isEmpty()) {
                return PageResult.empty();
            }
            wrapper.in(Contract::getId, contractIds);
        }
        
        wrapper.orderByDesc(Contract::getSignDate);
        
        IPage<Contract> page = contractRepository.page(
            new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        
        List<AdminContractViewDTO> records = page.getRecords().stream()
            .map(this::toAdminViewDTO)
            .collect(Collectors.toList());
        
        // 按委托人名称筛选（需要在内存中过滤，因为客户名称在另一个表）
        if (StringUtils.hasText(query.getClientName())) {
            records = records.stream()
                .filter(dto -> dto.getClientName() != null && 
                              dto.getClientName().contains(query.getClientName()))
                .collect(Collectors.toList());
        }
        
        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取合同详情（只读）
     * 
     * Requirements: 5.5, 8.1
     */
    public AdminContractViewDTO getContractById(Long id) {
        // 数据权限检查 (Requirement 8.1)
        if (!dataPermissionService.canAccessContract(id)) {
            log.warn("用户无权访问合同: contractId={}", id);
            return null;
        }
        
        Contract contract = contractRepository.getByIdOrThrow(id, "合同不存在");
        
        // 只允许查看已审批通过的合同
        if (!"ACTIVE".equals(contract.getStatus())) {
            return null;
        }
        
        return toAdminViewDTO(contract);
    }

    /**
     * 查询指定月份的合同列表（用于司法局报备导出）
     * 
     * Requirements: 6.1
     */
    public List<AdminContractViewDTO> listContractsByMonth(int year, int month) {
        java.time.LocalDate startDate = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        
        AdminContractQueryDTO query = new AdminContractQueryDTO();
        query.setSignDateFrom(startDate);
        query.setSignDateTo(endDate);
        query.setPageNum(1);
        query.setPageSize(10000); // 导出不分页
        
        PageResult<AdminContractViewDTO> result = listApprovedContracts(query);
        return result.getRecords();
    }

    /**
     * 转换为行政视图DTO（只包含行政需要的字段）
     * 
     * Requirements: 5.2, 5.3
     */
    private AdminContractViewDTO toAdminViewDTO(Contract contract) {
        AdminContractViewDTO dto = new AdminContractViewDTO();
        dto.setId(contract.getId());
        dto.setContractNo(contract.getContractNo());
        dto.setName(contract.getName());
        dto.setSignDate(contract.getSignDate());
        dto.setCaseType(contract.getCaseType());
        dto.setCaseTypeName(MatterConstants.getCaseTypeName(contract.getCaseType()));
        dto.setCauseOfAction(contract.getCauseOfAction());
        dto.setCauseOfActionName(getCauseOfActionName(contract.getCauseOfAction()));
        dto.setOpposingParty(contract.getOpposingParty());
        dto.setTotalAmount(contract.getTotalAmount());
        dto.setStatus(contract.getStatus());
        
        // 诉讼类型额外字段
        dto.setJurisdictionCourt(contract.getJurisdictionCourt());
        dto.setTrialStage(contract.getTrialStage());
        dto.setTrialStageName(getTrialStageName(contract.getTrialStage()));
        
        // 客户信息
        if (contract.getClientId() != null) {
            dto.setClientId(contract.getClientId());
            Client client = clientRepository.findById(contract.getClientId());
            if (client != null) {
                dto.setClientName(client.getName());
            }
        }
        
        // 承办律师信息
        List<ContractParticipant> participants = participantRepository.findByContractId(contract.getId());
        ContractParticipant lead = participants.stream()
            .filter(p -> "LEAD".equals(p.getRole()))
            .findFirst().orElse(null);
        if (lead != null) {
            dto.setLeadLawyerId(lead.getUserId());
            User lawyer = userRepository.findById(lead.getUserId());
            if (lawyer != null) {
                dto.setLeadLawyerName(lawyer.getRealName());
            }
        }
        
        dto.setCreatedAt(contract.getCreatedAt());
        dto.setUpdatedAt(contract.getUpdatedAt());
        
        return dto;
    }


    /**
     * 获取审理阶段名称（支持多选，逗号分隔）
     */
    private String getTrialStageName(String trialStage) {
        if (trialStage == null || trialStage.isEmpty()) return null;
        // 支持多选：逗号分隔的多个值
        return java.util.Arrays.stream(trialStage.split(","))
                .map(stage -> switch (stage.trim()) {
                    // 通用阶段
                    case "FIRST_INSTANCE" -> "一审";
                    case "SECOND_INSTANCE" -> "二审";
                    case "RETRIAL" -> "再审";
                    case "EXECUTION" -> "执行";
                    case "NON_LITIGATION" -> "非诉服务";
                    case "ARBITRATION" -> "仲裁阶段";
                    // 刑事案件阶段
                    case "INVESTIGATION" -> "侦查阶段";
                    case "PROSECUTION_REVIEW" -> "审查起诉";
                    case "DEATH_PENALTY_REVIEW" -> "死刑复核";
                    // 行政案件阶段
                    case "ADMINISTRATIVE_RECONSIDERATION" -> "行政复议";
                    // 执行案件阶段
                    case "EXECUTION_OBJECTION" -> "执行异议";
                    case "EXECUTION_REVIEW" -> "执行复议";
                    default -> stage;
                })
                .collect(java.util.stream.Collectors.joining("、"));
    }

    /**
     * 获取案由名称
     * 案由存储的是ID或名称，需要根据实际情况处理
     */
    private String getCauseOfActionName(String causeOfAction) {
        if (causeOfAction == null || causeOfAction.isEmpty()) {
            return null;
        }
        // 如果是纯数字，说明存储的是案由ID，需要查询案由名称
        // 这里简化处理，直接返回原值，实际应该查询案由表
        // TODO: 如果有案由表，应该查询案由名称
        try {
            Long.parseLong(causeOfAction);
            // 是数字ID，暂时返回原值，后续可以查询案由表
            return causeOfAction;
        } catch (NumberFormatException e) {
            // 不是数字，说明存储的就是案由名称
            return causeOfAction;
        }
    }
}
