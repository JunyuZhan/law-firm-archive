package com.lawfirm.application.matter.service;

import com.lawfirm.application.matter.command.CreateStateCompensationCommand;
import com.lawfirm.application.matter.command.UpdateStateCompensationCommand;
import com.lawfirm.application.matter.dto.StateCompensationDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterStateCompensation;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.MatterStateCompensationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 国家赔偿案件应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateCompensationAppService {

    private final MatterStateCompensationRepository stateCompensationRepository;
    private final MatterRepository matterRepository;
    private final MatterAppService matterAppService;

    /**
     * 根据案件ID获取国家赔偿信息
     */
    public StateCompensationDTO getByMatterId(Long matterId) {
        // 验证案件存在
        Matter matter = matterRepository.getByIdOrThrow(matterId, "案件不存在");

        MatterStateCompensation entity = stateCompensationRepository.findByMatterId(matterId);
        if (entity == null) {
            return null;
        }

        return toDTO(entity, matter);
    }

    /**
     * 创建国家赔偿信息
     */
    @Transactional
    public StateCompensationDTO create(CreateStateCompensationCommand command) {
        // 验证案件存在
        Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");
        
        // 验证数据权限：只有案件负责人或参与者才能操作
        matterAppService.validateMatterOwnership(command.getMatterId());

        // 检查是否已存在
        if (stateCompensationRepository.existsByMatterId(command.getMatterId())) {
            throw new BusinessException("该案件已存在国家赔偿信息");
        }

        // 验证案件类型
        String caseType = matter.getCaseType();
        if (!"STATE_COMP_ADMIN".equals(caseType) && !"STATE_COMP_CRIMINAL".equals(caseType)) {
            throw new BusinessException("只有国家赔偿类型的案件才能添加国家赔偿信息");
        }

        // 刑事赔偿必须填写刑事诉讼终结
        if ("STATE_COMP_CRIMINAL".equals(caseType) && !Boolean.TRUE.equals(command.getCriminalCaseTerminated())) {
            throw new BusinessException("刑事赔偿案件必须确认刑事诉讼已终结");
        }

        MatterStateCompensation entity = MatterStateCompensation.builder()
                .matterId(command.getMatterId())
                .obligorOrgName(command.getObligorOrgName())
                .obligorOrgType(command.getObligorOrgType())
                .caseSource(command.getCaseSource())
                .damageDescription(command.getDamageDescription())
                .criminalCaseTerminated(command.getCriminalCaseTerminated())
                .criminalCaseNo(command.getCriminalCaseNo())
                .compensationCommittee(command.getCompensationCommittee())
                .applicationDate(command.getApplicationDate())
                .acceptanceDate(command.getAcceptanceDate())
                .decisionDate(command.getDecisionDate())
                .reconsiderationDate(command.getReconsiderationDate())
                .reconsiderationDecisionDate(command.getReconsiderationDecisionDate())
                .committeeAppDate(command.getCommitteeAppDate())
                .committeeDecisionDate(command.getCommitteeDecisionDate())
                .adminLitigationFilingDate(command.getAdminLitigationFilingDate())
                .adminLitigationCourtName(command.getAdminLitigationCourtName())
                .claimAmount(command.getClaimAmount())
                .compensationItems(command.getCompensationItems())
                .decisionResult(command.getDecisionResult())
                .approvedAmount(command.getApprovedAmount())
                .paymentStatus(command.getPaymentStatus())
                .paymentDate(command.getPaymentDate())
                .remark(command.getRemark())
                .createdBy(SecurityUtils.getUserId())
                .updatedBy(SecurityUtils.getUserId())
                .build();

        stateCompensationRepository.save(entity);
        log.info("创建国家赔偿信息成功: matterId={}", command.getMatterId());

        return toDTO(entity, matter);
    }

    /**
     * 更新国家赔偿信息
     */
    @Transactional
    public StateCompensationDTO update(UpdateStateCompensationCommand command) {
        MatterStateCompensation entity = stateCompensationRepository.getByIdOrThrow(command.getId(), "国家赔偿信息不存在");

        // 验证案件类型
        Matter matter = matterRepository.getByIdOrThrow(entity.getMatterId(), "关联案件不存在");
        
        // 验证数据权限：只有案件负责人或参与者才能操作
        matterAppService.validateMatterOwnership(entity.getMatterId());
        String caseType = matter.getCaseType();

        // 刑事赔偿必须填写刑事诉讼终结
        if ("STATE_COMP_CRIMINAL".equals(caseType)) {
            Boolean terminated = command.getCriminalCaseTerminated();
            if (terminated == null) {
                terminated = entity.getCriminalCaseTerminated();
            }
            if (!Boolean.TRUE.equals(terminated)) {
                throw new BusinessException("刑事赔偿案件必须确认刑事诉讼已终结");
            }
        }

        // 更新字段
        if (command.getObligorOrgName() != null) {
            entity.setObligorOrgName(command.getObligorOrgName());
        }
        if (command.getObligorOrgType() != null) {
            entity.setObligorOrgType(command.getObligorOrgType());
        }
        if (command.getCaseSource() != null) {
            entity.setCaseSource(command.getCaseSource());
        }
        if (command.getDamageDescription() != null) {
            entity.setDamageDescription(command.getDamageDescription());
        }
        if (command.getCriminalCaseTerminated() != null) {
            entity.setCriminalCaseTerminated(command.getCriminalCaseTerminated());
        }
        if (command.getCriminalCaseNo() != null) {
            entity.setCriminalCaseNo(command.getCriminalCaseNo());
        }
        if (command.getCompensationCommittee() != null) {
            entity.setCompensationCommittee(command.getCompensationCommittee());
        }
        if (command.getApplicationDate() != null) {
            entity.setApplicationDate(command.getApplicationDate());
        }
        if (command.getAcceptanceDate() != null) {
            entity.setAcceptanceDate(command.getAcceptanceDate());
        }
        if (command.getDecisionDate() != null) {
            entity.setDecisionDate(command.getDecisionDate());
        }
        if (command.getReconsiderationDate() != null) {
            entity.setReconsiderationDate(command.getReconsiderationDate());
        }
        if (command.getReconsiderationDecisionDate() != null) {
            entity.setReconsiderationDecisionDate(command.getReconsiderationDecisionDate());
        }
        if (command.getCommitteeAppDate() != null) {
            entity.setCommitteeAppDate(command.getCommitteeAppDate());
        }
        if (command.getCommitteeDecisionDate() != null) {
            entity.setCommitteeDecisionDate(command.getCommitteeDecisionDate());
        }
        if (command.getAdminLitigationFilingDate() != null) {
            entity.setAdminLitigationFilingDate(command.getAdminLitigationFilingDate());
        }
        if (command.getAdminLitigationCourtName() != null) {
            entity.setAdminLitigationCourtName(command.getAdminLitigationCourtName());
        }
        if (command.getClaimAmount() != null) {
            entity.setClaimAmount(command.getClaimAmount());
        }
        if (command.getCompensationItems() != null) {
            entity.setCompensationItems(command.getCompensationItems());
        }
        if (command.getDecisionResult() != null) {
            entity.setDecisionResult(command.getDecisionResult());
        }
        if (command.getApprovedAmount() != null) {
            entity.setApprovedAmount(command.getApprovedAmount());
        }
        if (command.getPaymentStatus() != null) {
            entity.setPaymentStatus(command.getPaymentStatus());
        }
        if (command.getPaymentDate() != null) {
            entity.setPaymentDate(command.getPaymentDate());
        }
        if (command.getRemark() != null) {
            entity.setRemark(command.getRemark());
        }

        stateCompensationRepository.updateById(entity);
        log.info("更新国家赔偿信息成功: id={}", command.getId());

        return toDTO(entity, matter);
    }

    /**
     * 删除国家赔偿信息
     */
    @Transactional
    public void delete(Long id) {
        MatterStateCompensation entity = stateCompensationRepository.getByIdOrThrow(id, "国家赔偿信息不存在");
        
        // 验证数据权限：只有案件负责人或参与者才能操作
        matterAppService.validateMatterOwnership(entity.getMatterId());
        
        stateCompensationRepository.removeById(id);
        log.info("删除国家赔偿信息成功: id={}, matterId={}", id, entity.getMatterId());
    }

    /**
     * 根据案件ID删除国家赔偿信息
     */
    @Transactional
    public void deleteByMatterId(Long matterId) {
        // 验证数据权限：只有案件负责人或参与者才能操作
        matterAppService.validateMatterOwnership(matterId);
        
        stateCompensationRepository.deleteByMatterId(matterId);
        log.info("删除国家赔偿信息成功: matterId={}", matterId);
    }

    /**
     * 实体转DTO
     */
    private StateCompensationDTO toDTO(MatterStateCompensation entity, Matter matter) {
        StateCompensationDTO dto = new StateCompensationDTO();
        dto.setId(entity.getId());
        dto.setMatterId(entity.getMatterId());
        dto.setMatterNo(matter.getMatterNo());
        dto.setMatterName(matter.getName());

        dto.setObligorOrgName(entity.getObligorOrgName());
        dto.setObligorOrgType(entity.getObligorOrgType());
        dto.setObligorOrgTypeName(getObligorOrgTypeName(entity.getObligorOrgType()));

        dto.setCaseSource(entity.getCaseSource());
        dto.setCaseSourceName(getCaseSourceName(entity.getCaseSource()));
        dto.setDamageDescription(entity.getDamageDescription());

        dto.setCriminalCaseTerminated(entity.getCriminalCaseTerminated());
        dto.setCriminalCaseNo(entity.getCriminalCaseNo());
        dto.setCompensationCommittee(entity.getCompensationCommittee());

        dto.setApplicationDate(entity.getApplicationDate());
        dto.setAcceptanceDate(entity.getAcceptanceDate());
        dto.setDecisionDate(entity.getDecisionDate());
        dto.setReconsiderationDate(entity.getReconsiderationDate());
        dto.setReconsiderationDecisionDate(entity.getReconsiderationDecisionDate());
        dto.setCommitteeAppDate(entity.getCommitteeAppDate());
        dto.setCommitteeDecisionDate(entity.getCommitteeDecisionDate());
        dto.setAdminLitigationFilingDate(entity.getAdminLitigationFilingDate());
        dto.setAdminLitigationCourtName(entity.getAdminLitigationCourtName());

        dto.setClaimAmount(entity.getClaimAmount());
        dto.setCompensationItems(entity.getCompensationItems());

        dto.setDecisionResult(entity.getDecisionResult());
        dto.setDecisionResultName(getDecisionResultName(entity.getDecisionResult()));
        dto.setApprovedAmount(entity.getApprovedAmount());
        dto.setPaymentStatus(entity.getPaymentStatus());
        dto.setPaymentStatusName(getPaymentStatusName(entity.getPaymentStatus()));
        dto.setPaymentDate(entity.getPaymentDate());

        dto.setRemark(entity.getRemark());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /**
     * 获取义务机关类型名称
     */
    private String getObligorOrgTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case MatterStateCompensation.ObligorOrgType.PUBLIC_SECURITY -> "公安机关";
            case MatterStateCompensation.ObligorOrgType.PROCURATORATE -> "检察机关";
            case MatterStateCompensation.ObligorOrgType.COURT -> "审判机关";
            case MatterStateCompensation.ObligorOrgType.PRISON -> "监狱管理机关";
            case MatterStateCompensation.ObligorOrgType.ADMIN_ORGAN -> "行政机关";
            case MatterStateCompensation.ObligorOrgType.OTHER -> "其他";
            default -> type;
        };
    }

    /**
     * 获取致损行为类型名称
     */
    private String getCaseSourceName(String source) {
        if (source == null) return null;
        return switch (source) {
            case MatterStateCompensation.DamageCauseType.ILLEGAL_DETENTION -> "违法拘留";
            case MatterStateCompensation.DamageCauseType.ILLEGAL_COERCIVE -> "违法采取强制措施";
            case MatterStateCompensation.DamageCauseType.ILLEGAL_WEAPON -> "违法使用武器警械";
            case MatterStateCompensation.DamageCauseType.ILLEGAL_SEARCH -> "违法搜查";
            case MatterStateCompensation.DamageCauseType.WRONGFUL_CONVICT -> "错误判决";
            case MatterStateCompensation.DamageCauseType.ILLEGAL_DETENTION_PROPERTY -> "违法查封/扣押/冻结财产";
            case MatterStateCompensation.DamageCauseType.ILLEGAL_ADMIN_PUNISHMENT -> "违法行政处罚";
            case MatterStateCompensation.DamageCauseType.OTHER -> "其他";
            default -> source;
        };
    }

    /**
     * 获取决定结果名称
     */
    private String getDecisionResultName(String result) {
        if (result == null) return null;
        return switch (result) {
            case MatterStateCompensation.DecisionResult.GRANTED -> "全部支持";
            case MatterStateCompensation.DecisionResult.DENIED -> "不予赔偿";
            case MatterStateCompensation.DecisionResult.PARTIAL_GRANTED -> "部分支持";
            default -> result;
        };
    }

    /**
     * 获取支付状态名称
     */
    private String getPaymentStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case MatterStateCompensation.PaymentStatus.UNPAID -> "未支付";
            case MatterStateCompensation.PaymentStatus.PAID -> "已支付";
            case MatterStateCompensation.PaymentStatus.PARTIAL_PAID -> "部分支付";
            default -> status;
        };
    }
}
