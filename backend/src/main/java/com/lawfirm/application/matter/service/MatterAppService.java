package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.command.UpdateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.dto.MatterParticipantDTO;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.application.matter.command.CloseMatterCommand;
import com.lawfirm.application.matter.service.DeadlineAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterParticipantMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 案件应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatterAppService {

    private final MatterRepository matterRepository;
    private final MatterMapper matterMapper;
    private final MatterParticipantMapper participantMapper;
    private final ClientRepository clientRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;
    private final ArchiveAppService archiveAppService;
    private final DeadlineAppService deadlineAppService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询案件
     */
    public PageResult<MatterDTO> listMatters(MatterQueryDTO query) {
        IPage<Matter> page;
        
        if (Boolean.TRUE.equals(query.getMyMatters())) {
            // 查询我参与的案件
            Long userId = SecurityUtils.getUserId();
            page = matterMapper.selectByParticipantUserId(
                    new Page<>(query.getPageNum(), query.getPageSize()),
                    userId
            );
        } else {
            page = matterMapper.selectMatterPage(
                    new Page<>(query.getPageNum(), query.getPageSize()),
                    query.getName(),
                    query.getMatterNo(),
                    query.getClientId(),
                    query.getLeadLawyerId(),
                    query.getDepartmentId(),
                    query.getMatterType(),
                    query.getStatus()
            );
        }

        List<MatterDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建案件
     */
    @Transactional
    public MatterDTO createMatter(CreateMatterCommand command) {
        // 1. 验证客户存在
        clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");

        // 2. 生成案件编号
        String matterNo = generateMatterNo(command.getMatterType());

        // 3. 创建案件实体
        Matter matter = Matter.builder()
                .matterNo(matterNo)
                .name(command.getName())
                .matterType(command.getMatterType())
                .businessType(command.getBusinessType())
                .clientId(command.getClientId())
                .opposingParty(command.getOpposingParty())
                .opposingLawyerName(command.getOpposingLawyerName())
                .opposingLawyerLicenseNo(command.getOpposingLawyerLicenseNo())
                .opposingLawyerFirm(command.getOpposingLawyerFirm())
                .opposingLawyerPhone(command.getOpposingLawyerPhone())
                .opposingLawyerEmail(command.getOpposingLawyerEmail())
                .description(command.getDescription())
                .status("DRAFT")
                .originatorId(command.getOriginatorId() != null ? command.getOriginatorId() : SecurityUtils.getUserId())
                .leadLawyerId(command.getLeadLawyerId())
                .departmentId(command.getDepartmentId() != null ? command.getDepartmentId() : SecurityUtils.getDepartmentId())
                .feeType(command.getFeeType())
                .estimatedFee(command.getEstimatedFee())
                .filingDate(command.getFilingDate())
                .expectedClosingDate(command.getExpectedClosingDate())
                .claimAmount(command.getClaimAmount())
                .contractId(command.getContractId())
                .remark(command.getRemark())
                .conflictStatus("PENDING")
                .build();

        // 4. 保存案件
        matterRepository.save(matter);

        // 5. 添加团队成员
        if (command.getParticipants() != null && !command.getParticipants().isEmpty()) {
            for (CreateMatterCommand.ParticipantCommand pc : command.getParticipants()) {
                addParticipant(matter.getId(), pc.getUserId(), pc.getRole(), 
                        pc.getCommissionRate(), pc.getIsOriginator());
            }
        }

        // 6. 如果指定了主办律师，自动添加为团队成员
        if (command.getLeadLawyerId() != null) {
            if (participantMapper.countByMatterIdAndUserId(matter.getId(), command.getLeadLawyerId()) == 0) {
                addParticipant(matter.getId(), command.getLeadLawyerId(), "LEAD", null, false);
            }
        }

        // 7. 如果是诉讼类项目且有立案日期，自动创建期限提醒
        if ("LITIGATION".equals(matter.getMatterType()) && matter.getFilingDate() != null) {
            try {
                deadlineAppService.autoCreateDeadlines(matter.getId());
            } catch (Exception e) {
                log.warn("自动创建期限提醒失败: matterId={}", matter.getId(), e);
            }
        }

        log.info("案件创建成功: {} ({})", matter.getName(), matter.getMatterNo());
        return toDTO(matter);
    }

    /**
     * 更新案件
     */
    @Transactional
    public MatterDTO updateMatter(UpdateMatterCommand command) {
        Matter matter = matterRepository.getByIdOrThrow(command.getId(), "案件不存在");

        // 更新字段
        if (StringUtils.hasText(command.getName())) {
            matter.setName(command.getName());
        }
        if (StringUtils.hasText(command.getMatterType())) {
            matter.setMatterType(command.getMatterType());
        }
        if (command.getBusinessType() != null) {
            matter.setBusinessType(command.getBusinessType());
        }
        if (command.getClientId() != null) {
            clientRepository.getByIdOrThrow(command.getClientId(), "客户不存在");
            matter.setClientId(command.getClientId());
        }
        if (command.getOpposingParty() != null) {
            matter.setOpposingParty(command.getOpposingParty());
        }
        if (command.getOpposingLawyerName() != null) {
            matter.setOpposingLawyerName(command.getOpposingLawyerName());
        }
        if (command.getOpposingLawyerLicenseNo() != null) {
            matter.setOpposingLawyerLicenseNo(command.getOpposingLawyerLicenseNo());
        }
        if (command.getOpposingLawyerFirm() != null) {
            matter.setOpposingLawyerFirm(command.getOpposingLawyerFirm());
        }
        if (command.getOpposingLawyerPhone() != null) {
            matter.setOpposingLawyerPhone(command.getOpposingLawyerPhone());
        }
        if (command.getOpposingLawyerEmail() != null) {
            matter.setOpposingLawyerEmail(command.getOpposingLawyerEmail());
        }
        if (command.getDescription() != null) {
            matter.setDescription(command.getDescription());
        }
        if (command.getOriginatorId() != null) {
            matter.setOriginatorId(command.getOriginatorId());
        }
        if (command.getLeadLawyerId() != null) {
            matter.setLeadLawyerId(command.getLeadLawyerId());
        }
        if (command.getDepartmentId() != null) {
            matter.setDepartmentId(command.getDepartmentId());
        }
        if (command.getFeeType() != null) {
            matter.setFeeType(command.getFeeType());
        }
        if (command.getEstimatedFee() != null) {
            matter.setEstimatedFee(command.getEstimatedFee());
        }
        if (command.getActualFee() != null) {
            matter.setActualFee(command.getActualFee());
        }
        if (command.getFilingDate() != null) {
            matter.setFilingDate(command.getFilingDate());
        }
        if (command.getExpectedClosingDate() != null) {
            matter.setExpectedClosingDate(command.getExpectedClosingDate());
        }
        if (command.getActualClosingDate() != null) {
            matter.setActualClosingDate(command.getActualClosingDate());
        }
        if (command.getClaimAmount() != null) {
            matter.setClaimAmount(command.getClaimAmount());
        }
        if (command.getOutcome() != null) {
            matter.setOutcome(command.getOutcome());
        }
        if (command.getContractId() != null) {
            matter.setContractId(command.getContractId());
        }
        if (command.getRemark() != null) {
            matter.setRemark(command.getRemark());
        }

        matterRepository.updateById(matter);
        log.info("案件更新成功: {}", matter.getName());
        return toDTO(matter);
    }

    /**
     * 删除案件
     */
    @Transactional
    public void deleteMatter(Long id) {
        Matter matter = matterRepository.getByIdOrThrow(id, "案件不存在");
        
        if (!"DRAFT".equals(matter.getStatus())) {
            throw new BusinessException("只有草稿状态的案件可以删除");
        }

        // 删除团队成员
        participantMapper.deleteByMatterId(id);
        // 删除案件
        matterMapper.deleteById(id);
        log.info("案件删除成功: {}", matter.getName());
    }

    /**
     * 获取案件详情
     */
    public MatterDTO getMatterById(Long id) {
        Matter matter = matterRepository.getByIdOrThrow(id, "案件不存在");
        MatterDTO dto = toDTO(matter);
        
        // 加载团队成员
        List<MatterParticipant> participants = participantMapper.selectByMatterId(id);
        dto.setParticipants(participants.stream()
                .map(this::toParticipantDTO)
                .collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 修改案件状态
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        Matter matter = matterRepository.getByIdOrThrow(id, "案件不存在");
        
        // 状态流转验证
        validateStatusTransition(matter.getStatus(), status);
        
        matter.setStatus(status);
        if ("CLOSED".equals(status) && matter.getActualClosingDate() == null) {
            matter.setActualClosingDate(LocalDate.now());
        }
        
        matterRepository.updateById(matter);
        log.info("案件状态修改成功: {} -> {}", matter.getName(), status);
    }

    /**
     * 添加团队成员
     */
    @Transactional
    public void addParticipant(Long matterId, Long userId, String role, 
                                java.math.BigDecimal commissionRate, Boolean isOriginator) {
        // 检查是否已在团队中
        if (participantMapper.countByMatterIdAndUserId(matterId, userId) > 0) {
            throw new BusinessException("该成员已在案件团队中");
        }

        MatterParticipant participant = MatterParticipant.builder()
                .matterId(matterId)
                .userId(userId)
                .role(role)
                .commissionRate(commissionRate)
                .isOriginator(isOriginator != null ? isOriginator : false)
                .joinDate(LocalDate.now())
                .status("ACTIVE")
                .build();

        participantMapper.insert(participant);
        log.info("添加案件团队成员: matterId={}, userId={}, role={}", matterId, userId, role);
    }

    /**
     * 移除团队成员
     */
    @Transactional
    public void removeParticipant(Long matterId, Long userId) {
        participantMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MatterParticipant>()
                        .eq(MatterParticipant::getMatterId, matterId)
                        .eq(MatterParticipant::getUserId, userId)
        );
        log.info("移除案件团队成员: matterId={}, userId={}", matterId, userId);
    }

    /**
     * 生成案件编号
     */
    private String generateMatterNo(String type) {
        String prefix = "LITIGATION".equals(type) ? "L" : "N";
        String datePart = LocalDate.now().toString().replace("-", "").substring(2);
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + datePart + random;
    }

    /**
     * 验证状态流转
     */
    private void validateStatusTransition(String from, String to) {
        // 简化的状态机验证
        // DRAFT -> PENDING -> ACTIVE -> SUSPENDED/CLOSED -> ARCHIVED
        // 实际项目中可以更复杂
    }

    /**
     * 获取案件类型名称
     */
    private String getMatterTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "LITIGATION" -> "诉讼";
            case "NON_LITIGATION" -> "非诉";
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
            case "ACTIVE" -> "进行中";
            case "SUSPENDED" -> "暂停";
            case "CLOSED" -> "结案";
            case "ARCHIVED" -> "归档";
            default -> status;
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
     * 获取角色名称
     */
    private String getRoleName(String role) {
        if (role == null) return null;
        return switch (role) {
            case "LEAD" -> "主办律师";
            case "CO_COUNSEL" -> "协办律师";
            case "PARALEGAL" -> "律师助理";
            case "TRAINEE" -> "实习律师";
            default -> role;
        };
    }

    /**
     * Matter Entity 转 DTO
     */
    private MatterDTO toDTO(Matter matter) {
        MatterDTO dto = new MatterDTO();
        dto.setId(matter.getId());
        dto.setMatterNo(matter.getMatterNo());
        dto.setName(matter.getName());
        dto.setMatterType(matter.getMatterType());
        dto.setMatterTypeName(getMatterTypeName(matter.getMatterType()));
        dto.setBusinessType(matter.getBusinessType());
        dto.setClientId(matter.getClientId());
        dto.setOpposingParty(matter.getOpposingParty());
        dto.setOpposingLawyerName(matter.getOpposingLawyerName());
        dto.setOpposingLawyerLicenseNo(matter.getOpposingLawyerLicenseNo());
        dto.setOpposingLawyerFirm(matter.getOpposingLawyerFirm());
        dto.setOpposingLawyerPhone(matter.getOpposingLawyerPhone());
        dto.setOpposingLawyerEmail(matter.getOpposingLawyerEmail());
        dto.setDescription(matter.getDescription());
        dto.setStatus(matter.getStatus());
        dto.setStatusName(getStatusName(matter.getStatus()));
        dto.setOriginatorId(matter.getOriginatorId());
        dto.setLeadLawyerId(matter.getLeadLawyerId());
        dto.setDepartmentId(matter.getDepartmentId());
        dto.setFeeType(matter.getFeeType());
        dto.setFeeTypeName(getFeeTypeName(matter.getFeeType()));
        dto.setEstimatedFee(matter.getEstimatedFee());
        dto.setActualFee(matter.getActualFee());
        dto.setFilingDate(matter.getFilingDate());
        dto.setExpectedClosingDate(matter.getExpectedClosingDate());
        dto.setActualClosingDate(matter.getActualClosingDate());
        dto.setClaimAmount(matter.getClaimAmount());
        dto.setOutcome(matter.getOutcome());
        dto.setContractId(matter.getContractId());
        dto.setRemark(matter.getRemark());
        dto.setConflictStatus(matter.getConflictStatus());
        dto.setCreatedAt(matter.getCreatedAt());
        dto.setUpdatedAt(matter.getUpdatedAt());
        return dto;
    }

    /**
     * Participant Entity 转 DTO
     */
    private MatterParticipantDTO toParticipantDTO(MatterParticipant p) {
        MatterParticipantDTO dto = new MatterParticipantDTO();
        dto.setId(p.getId());
        dto.setMatterId(p.getMatterId());
        dto.setUserId(p.getUserId());
        dto.setRole(p.getRole());
        dto.setRoleName(getRoleName(p.getRole()));
        dto.setCommissionRate(p.getCommissionRate());
        dto.setIsOriginator(p.getIsOriginator());
        dto.setJoinDate(p.getJoinDate());
        dto.setExitDate(p.getExitDate());
        dto.setStatus(p.getStatus());
        dto.setRemark(p.getRemark());
        return dto;
    }

    /**
     * 申请项目结案
     */
    @Transactional(rollbackFor = Exception.class)
    public MatterDTO applyCloseMatter(CloseMatterCommand command) {
        Matter matter = matterRepository.findById(command.getMatterId());
        if (matter == null) {
            throw new BusinessException("项目不存在");
        }

        if (!"ACTIVE".equals(matter.getStatus()) && !"SUSPENDED".equals(matter.getStatus())) {
            throw new BusinessException("只有进行中或暂停状态的项目才能申请结案");
        }

        // 更新项目信息
        matter.setActualClosingDate(command.getClosingDate());
        matter.setOutcome(command.getOutcome());
        if (command.getRemark() != null) {
            matter.setRemark((matter.getRemark() != null ? matter.getRemark() + "\n" : "") + 
                    "结案申请: " + command.getClosingReason() + 
                    (command.getSummary() != null ? "\n结案总结: " + command.getSummary() : ""));
        }
        matter.setStatus("PENDING_CLOSE");  // 待审批结案状态
        matter.setUpdatedAt(java.time.LocalDateTime.now());
        matter.setUpdatedBy(SecurityUtils.getUserId());
        matterRepository.getBaseMapper().updateById(matter);

        // 创建结案审批记录
        try {
            Long approverId = approverService.findDefaultApprover();
            String businessSnapshot = objectMapper.writeValueAsString(matter);
            approvalService.createApproval(
                    "MATTER_CLOSE",
                    matter.getId(),
                    matter.getMatterNo(),
                    "项目结案申请：" + matter.getName(),
                    approverId,
                    "NORMAL",
                    "NORMAL",
                    businessSnapshot
            );
        } catch (Exception e) {
            log.error("创建结案审批记录失败", e);
            // 不阻断主流程
        }

        log.info("申请项目结案: matterId={}, matterNo={}", command.getMatterId(), matter.getMatterNo());

        return toDTO(matter);
    }

    /**
     * 审批项目结案
     */
    @Transactional(rollbackFor = Exception.class)
    public MatterDTO approveCloseMatter(Long matterId, Boolean approved, String comment) {
        Matter matter = matterRepository.findById(matterId);
        if (matter == null) {
            throw new BusinessException("项目不存在");
        }

        if (!"PENDING_CLOSE".equals(matter.getStatus())) {
            throw new BusinessException("项目不在待审批结案状态");
        }

        if (Boolean.TRUE.equals(approved)) {
            // 批准结案
            matter.setStatus("CLOSED");
            matter.setUpdatedAt(java.time.LocalDateTime.now());
            matter.setUpdatedBy(SecurityUtils.getUserId());
            matterRepository.getBaseMapper().updateById(matter);

            // 触发归档流程
            try {
                com.lawfirm.application.archive.command.CreateArchiveCommand archiveCmd = 
                        new com.lawfirm.application.archive.command.CreateArchiveCommand();
                archiveCmd.setMatterId(matterId);
                archiveAppService.createArchive(archiveCmd);
                log.info("项目结案后自动创建档案: matterId={}", matterId);
            } catch (Exception e) {
                log.error("创建档案失败", e);
                // 不阻断结案流程，仅记录日志
            }

            log.info("项目结案审批通过: matterId={}, matterNo={}", matterId, matter.getMatterNo());
        } else {
            // 驳回结案申请
            matter.setStatus("ACTIVE");  // 恢复为进行中状态
            matter.setUpdatedAt(java.time.LocalDateTime.now());
            matter.setUpdatedBy(SecurityUtils.getUserId());
            if (comment != null) {
                matter.setRemark((matter.getRemark() != null ? matter.getRemark() + "\n" : "") + 
                        "结案申请被驳回: " + comment);
            }
            matterRepository.getBaseMapper().updateById(matter);

            log.info("项目结案审批驳回: matterId={}, matterNo={}, comment={}", 
                    matterId, matter.getMatterNo(), comment);
        }

        return toDTO(matter);
    }

    /**
     * 生成结案报告（简化版，返回项目基本信息）
     */
    public String generateCloseReport(Long matterId) {
        Matter matter = matterRepository.findById(matterId);
        if (matter == null) {
            throw new BusinessException("项目不存在");
        }

        if (!"CLOSED".equals(matter.getStatus())) {
            throw new BusinessException("只有已结案的项目才能生成结案报告");
        }

        // 生成简单的结案报告文本
        StringBuilder report = new StringBuilder();
        report.append("项目结案报告\n");
        report.append("==================\n\n");
        report.append("项目编号: ").append(matter.getMatterNo()).append("\n");
        report.append("项目名称: ").append(matter.getName()).append("\n");
        report.append("项目类型: ").append(getMatterTypeName(matter.getMatterType())).append("\n");
        report.append("立案日期: ").append(matter.getFilingDate()).append("\n");
        report.append("结案日期: ").append(matter.getActualClosingDate()).append("\n");
        report.append("判决/调解结果: ").append(matter.getOutcome() != null ? matter.getOutcome() : "无").append("\n");
        report.append("实际收费: ").append(matter.getActualFee() != null ? matter.getActualFee() : "0").append("元\n");
        report.append("\n备注: ").append(matter.getRemark() != null ? matter.getRemark() : "无").append("\n");

        return report.toString();
    }
}

