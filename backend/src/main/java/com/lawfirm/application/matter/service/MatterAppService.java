package com.lawfirm.application.matter.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.matter.command.CreateMatterCommand;
import com.lawfirm.application.matter.command.UpdateMatterCommand;
import com.lawfirm.application.matter.dto.MatterDTO;
import com.lawfirm.application.matter.dto.MatterClientDTO;
import com.lawfirm.application.matter.dto.MatterParticipantDTO;
import com.lawfirm.application.matter.dto.MatterQueryDTO;
import com.lawfirm.application.archive.service.ArchiveAppService;
import com.lawfirm.application.matter.command.CloseMatterCommand;
import com.lawfirm.application.matter.service.DeadlineAppService;
import com.lawfirm.application.system.service.NotificationAppService;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.entity.MatterClient;
import com.lawfirm.domain.matter.entity.MatterParticipant;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.repository.MatterClientRepository;
import com.lawfirm.domain.system.repository.DepartmentRepository;
import com.lawfirm.domain.system.repository.UserRepository;
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
    private final MatterClientRepository matterClientRepository;
    private final MatterMapper matterMapper;
    private final MatterParticipantMapper participantMapper;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ContractRepository contractRepository;
    private final ContractParticipantRepository contractParticipantRepository;
    private final DepartmentRepository departmentRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;
    private final ArchiveAppService archiveAppService;
    private final DeadlineAppService deadlineAppService;
    private final NotificationAppService notificationAppService;
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

        // 2. 验证合同（项目必须关联已审批通过的合同）
        if (command.getContractId() == null) {
            throw new BusinessException("创建项目必须关联合同，请先创建并审批合同");
        }
        Contract contract = contractRepository.getByIdOrThrow(command.getContractId(), "合同不存在");
        if (!"ACTIVE".equals(contract.getStatus())) {
            throw new BusinessException("只能基于已审批通过的合同创建项目，当前合同状态：" + contract.getStatus());
        }
        // 注意：一个合同可以创建多个项目（比如常年法顾合同下有多个具体项目）
        // 所以不再检查 contract.getMatterId() != null
        // 验证客户一致性（主要客户必须与合同客户一致）
        if (!contract.getClientId().equals(command.getClientId())) {
            throw new BusinessException("项目主要客户必须与合同客户一致");
        }

        // 3. 生成案件编号
        String matterNo = generateMatterNo(command.getMatterType());

        // 3. 创建案件实体
        // 由于项目是基于已审批通过的合同创建的，直接设为进行中状态
        Matter matter = Matter.builder()
                .matterNo(matterNo)
                .name(command.getName())
                .matterType(command.getMatterType())
                .caseType(command.getCaseType())
                .causeOfAction(command.getCauseOfAction())
                .businessType(command.getBusinessType())
                .clientId(command.getClientId())
                .opposingParty(command.getOpposingParty())
                .opposingLawyerName(command.getOpposingLawyerName())
                .opposingLawyerLicenseNo(command.getOpposingLawyerLicenseNo())
                .opposingLawyerFirm(command.getOpposingLawyerFirm())
                .opposingLawyerPhone(command.getOpposingLawyerPhone())
                .opposingLawyerEmail(command.getOpposingLawyerEmail())
                .description(command.getDescription())
                .status("ACTIVE")  // 基于已审批合同创建，直接进行中
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

        // 4.1 关联合同到项目
        contract.setMatterId(matter.getId());
        contractRepository.updateById(contract);
        
        // 4.2 保存客户关联（多客户支持）
        saveMatterClients(matter.getId(), command);
        
        // 4.3 从合同复制参与人到项目（如果合同有参与人）
        copyContractParticipantsToMatter(contract.getId(), matter.getId());

        // 5. 添加团队成员
        if (command.getParticipants() != null && !command.getParticipants().isEmpty()) {
            for (CreateMatterCommand.ParticipantCommand pc : command.getParticipants()) {
                // 检查是否已从合同复制过来
                if (participantMapper.countByMatterIdAndUserId(matter.getId(), pc.getUserId()) == 0) {
                    addParticipant(matter.getId(), pc.getUserId(), pc.getRole(), 
                            pc.getCommissionRate(), pc.getIsOriginator());
                }
            }
        }

        // 6. 如果指定了主办律师，自动添加为团队成员
        if (command.getLeadLawyerId() != null) {
            if (participantMapper.countByMatterIdAndUserId(matter.getId(), command.getLeadLawyerId()) == 0) {
                addParticipant(matter.getId(), command.getLeadLawyerId(), "LEAD", null, false);
            }
        }
        
        // 7. 自动将创建者添加为团队成员（如果还没有添加）
        Long creatorId = SecurityUtils.getUserId();
        if (creatorId != null && participantMapper.countByMatterIdAndUserId(matter.getId(), creatorId) == 0) {
            // 如果创建者就是主办律师，不再重复添加
            if (!creatorId.equals(command.getLeadLawyerId())) {
                addParticipant(matter.getId(), creatorId, "CO_COUNSEL", null, false);
            }
        }

        // 8. 如果是诉讼类项目且有立案日期，自动创建期限提醒
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
     * 基于合同创建项目
     * 从已审批的合同自动填充项目信息
     */
    @Transactional
    public MatterDTO createMatterFromContract(Long contractId, CreateMatterCommand command) {
        // 验证合同存在
        Contract contract = contractRepository.getByIdOrThrow(contractId, "合同不存在");

        // 从合同自动填充项目信息
        command.setContractId(contractId);
        if (command.getClientId() == null) {
            command.setClientId(contract.getClientId());
        }
        if (command.getFeeType() == null) {
            command.setFeeType(contract.getFeeType());
        }
        if (command.getEstimatedFee() == null && contract.getTotalAmount() != null) {
            command.setEstimatedFee(contract.getTotalAmount());
        }
        if (command.getDepartmentId() == null) {
            command.setDepartmentId(contract.getDepartmentId());
        }

        // 创建项目（createMatter会验证合同状态并自动关联）
        return createMatter(command);
    }

    /**
     * 更新案件
     */
    @Transactional
    public MatterDTO updateMatter(UpdateMatterCommand command) {
        Matter matter = matterRepository.getByIdOrThrow(command.getId(), "案件不存在");
        
        // 归档的项目不能编辑
        if ("ARCHIVED".equals(matter.getStatus())) {
            throw new BusinessException("已归档的项目不能编辑");
        }

        // 更新字段
        if (StringUtils.hasText(command.getName())) {
            matter.setName(command.getName());
        }
        if (StringUtils.hasText(command.getMatterType())) {
            matter.setMatterType(command.getMatterType());
        }
        if (command.getCaseType() != null) {
            matter.setCaseType(command.getCaseType());
        }
        if (command.getCauseOfAction() != null) {
            matter.setCauseOfAction(command.getCauseOfAction());
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
        String oldStatus = matter.getStatus();
        
        // 状态流转验证
        validateStatusTransition(oldStatus, status);
        
        matter.setStatus(status);
        if ("CLOSED".equals(status) && matter.getActualClosingDate() == null) {
            matter.setActualClosingDate(LocalDate.now());
        }
        
        matterRepository.updateById(matter);
        log.info("案件状态修改成功: {} -> {}", matter.getName(), status);
        
        // 发送状态变更通知给项目参与人
        sendMatterStatusNotification(matter, oldStatus, status);
        
        // 如果状态改为 ARCHIVED，自动创建档案记录
        if ("ARCHIVED".equals(status)) {
            try {
                // 检查是否已存在档案
                if (archiveAppService.getArchiveByMatterId(id) == null) {
                    com.lawfirm.application.archive.command.CreateArchiveCommand archiveCmd = 
                            new com.lawfirm.application.archive.command.CreateArchiveCommand();
                    archiveCmd.setMatterId(id);
                    archiveCmd.setArchiveName(matter.getName() + " - 档案");
                    // 根据项目类型设置档案类型
                    if ("LITIGATION".equals(matter.getMatterType())) {
                        archiveCmd.setArchiveType("LITIGATION");
                    } else {
                        archiveCmd.setArchiveType("NON_LITIGATION");
                    }
                    archiveCmd.setRetentionPeriod("10_YEARS"); // 默认10年
                    archiveAppService.createArchiveFromMatter(archiveCmd);
                    log.info("项目归档后自动创建档案: matterId={}", id);
                }
            } catch (Exception e) {
                log.error("项目归档后自动创建档案失败", e);
                // 不抛出异常，避免影响状态变更
            }
        }
    }
    
    /**
     * 发送项目状态变更通知给所有参与人
     */
    private void sendMatterStatusNotification(Matter matter, String oldStatus, String newStatus) {
        try {
            Long currentUserId = SecurityUtils.getUserId();
            String currentUserName = SecurityUtils.getRealName();
            
            // 获取项目参与人
            List<MatterParticipant> participants = participantMapper.selectByMatterId(matter.getId());
            
            String statusName = getMatterStatusName(newStatus);
            String title = String.format("项目【%s】状态变更", matter.getName());
            String content = String.format("%s 将项目【%s】状态修改为：%s", 
                    currentUserName, matter.getName(), statusName);
            
            // 给所有参与人发送通知（排除操作人自己）
            for (MatterParticipant p : participants) {
                if (!p.getUserId().equals(currentUserId)) {
                    notificationAppService.sendSystemNotification(
                            p.getUserId(),
                            title,
                            content,
                            "MATTER",
                            matter.getId()
                    );
                }
            }
            
            log.info("项目状态变更通知已发送: matterId={}, status={}, 通知人数={}", 
                    matter.getId(), newStatus, participants.size());
        } catch (Exception e) {
            log.warn("发送项目状态变更通知失败: matterId={}", matter.getId(), e);
        }
    }
    
    /**
     * 获取状态名称
     */
    private String getMatterStatusName(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "ACTIVE" -> "进行中";
            case "SUSPENDED" -> "暂停";
            case "CLOSED" -> "已结案";
            case "ARCHIVED" -> "已归档";
            default -> status;
        };
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
        
        // 发送通知给被添加的成员
        try {
            Matter matter = matterRepository.findById(matterId);
            if (matter != null) {
                String currentUserName = SecurityUtils.getRealName();
                String roleName = getRoleName(role);
                String title = "您已被添加到项目";
                String content = String.format("%s 将您添加到项目【%s】，您的角色是：%s", 
                        currentUserName, matter.getName(), roleName);
                notificationAppService.sendSystemNotification(
                        userId,
                        title,
                        content,
                        "MATTER",
                        matterId
                );
            }
        } catch (Exception e) {
            log.warn("发送团队成员添加通知失败: matterId={}, userId={}", matterId, userId, e);
        }
    }
    
    /**
     * 获取角色名称
     */
    private String getRoleName(String role) {
        if (role == null) return "成员";
        return switch (role) {
            case "LEAD_LAWYER" -> "主办律师";
            case "ASSISTANT" -> "协办律师";
            case "PARALEGAL" -> "律师助理";
            default -> role;
        };
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
            case "LITIGATION" -> "诉讼案件";
            case "NON_LITIGATION" -> "非诉项目";
            default -> type;
        };
    }

    /**
     * 获取案件细分类型名称
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
            case "DUE_DILIGENCE" -> "尽职调查";
            case "CONTRACT_REVIEW" -> "合同审查";
            case "LEGAL_OPINION" -> "法律意见";
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
        dto.setCaseType(matter.getCaseType());
        dto.setCaseTypeName(getCaseTypeName(matter.getCaseType()));
        dto.setCauseOfAction(matter.getCauseOfAction());
        // 案由名称由前端根据code查找，后端只存储code
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
        
        // 查询关联数据
        if (matter.getClientId() != null) {
            var client = clientRepository.findById(matter.getClientId());
            if (client != null) {
                dto.setClientName(client.getName());
            }
        }
        
        // 加载多客户列表
        List<MatterClient> matterClients = matterClientRepository.findByMatterId(matter.getId());
        if (matterClients != null && !matterClients.isEmpty()) {
            dto.setClients(matterClients.stream().map(this::toMatterClientDTO).collect(Collectors.toList()));
        }
        
        if (matter.getOriginatorId() != null) {
            var user = userRepository.findById(matter.getOriginatorId());
            if (user != null) {
                dto.setOriginatorName(user.getRealName());
            }
        }
        if (matter.getLeadLawyerId() != null) {
            var user = userRepository.findById(matter.getLeadLawyerId());
            if (user != null) {
                dto.setLeadLawyerName(user.getRealName());
            }
        }
        if (matter.getDepartmentId() != null) {
            var dept = departmentRepository.findById(matter.getDepartmentId());
            if (dept != null) {
                dto.setDepartmentName(dept.getName());
            }
        }
        
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
     * MatterClient Entity 转 DTO
     */
    private MatterClientDTO toMatterClientDTO(MatterClient mc) {
        MatterClientDTO dto = new MatterClientDTO();
        dto.setId(mc.getId());
        dto.setMatterId(mc.getMatterId());
        dto.setClientId(mc.getClientId());
        dto.setClientRole(mc.getClientRole());
        dto.setClientRoleName(MatterClientDTO.getClientRoleName(mc.getClientRole()));
        dto.setIsPrimary(mc.getIsPrimary());
        
        // 查询客户信息
        if (mc.getClientId() != null) {
            var client = clientRepository.findById(mc.getClientId());
            if (client != null) {
                dto.setClientName(client.getName());
                dto.setClientType(client.getClientType());
            }
        }
        
        return dto;
    }

    /**
     * 保存项目的多客户关联
     */
    private void saveMatterClients(Long matterId, CreateMatterCommand command) {
        // 如果有显式的客户列表，使用列表
        if (command.getClients() != null && !command.getClients().isEmpty()) {
            boolean hasPrimary = false;
            for (CreateMatterCommand.ClientCommand cc : command.getClients()) {
                MatterClient mc = MatterClient.builder()
                        .matterId(matterId)
                        .clientId(cc.getClientId())
                        .clientRole(cc.getClientRole() != null ? cc.getClientRole() : "PLAINTIFF")
                        .isPrimary(Boolean.TRUE.equals(cc.getIsPrimary()))
                        .build();
                matterClientRepository.save(mc);
                if (Boolean.TRUE.equals(cc.getIsPrimary())) {
                    hasPrimary = true;
                }
            }
            // 如果没有指定主要客户，将第一个设为主要客户
            if (!hasPrimary) {
                List<MatterClient> clients = matterClientRepository.findByMatterId(matterId);
                if (!clients.isEmpty()) {
                    MatterClient first = clients.get(0);
                    first.setIsPrimary(true);
                    matterClientRepository.updateById(first);
                }
            }
        } else {
            // 向后兼容：如果只有单个clientId，创建一条记录
            if (command.getClientId() != null) {
                MatterClient mc = MatterClient.builder()
                        .matterId(matterId)
                        .clientId(command.getClientId())
                        .clientRole("PLAINTIFF")
                        .isPrimary(true)
                        .build();
                matterClientRepository.save(mc);
            }
        }
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

            // 触发归档流程（带重试机制）
            triggerArchiveWithRetry(matterId, matter, 3);

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

    /**
     * 触发归档流程（带重试机制）
     * @param matterId 项目ID
     * @param matter 项目实体
     * @param maxRetries 最大重试次数
     */
    private void triggerArchiveWithRetry(Long matterId, Matter matter, int maxRetries) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                com.lawfirm.application.archive.command.CreateArchiveCommand archiveCmd = 
                        new com.lawfirm.application.archive.command.CreateArchiveCommand();
                archiveCmd.setMatterId(matterId);
                archiveAppService.createArchive(archiveCmd);
                log.info("项目结案后自动创建档案成功: matterId={}, attempt={}", matterId, attempt);
                return; // 成功则直接返回
            } catch (Exception e) {
                lastException = e;
                log.warn("创建档案失败，尝试次数: {}/{}, matterId={}, error={}", 
                        attempt, maxRetries, matterId, e.getMessage());
                
                if (attempt < maxRetries) {
                    try {
                        // 等待一段时间后重试（指数退避）
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // 所有重试都失败，发送通知给主办律师和系统管理员
        log.error("创建档案失败，已达最大重试次数: matterId={}", matterId, lastException);
        sendArchiveFailureNotification(matter, lastException);
    }

    /**
     * 发送归档失败通知
     */
    private void sendArchiveFailureNotification(Matter matter, Exception e) {
        String errorMessage = e != null ? e.getMessage() : "未知错误";
        String content = String.format("项目【%s】（%s）结案后自动归档失败，请手动处理。\n错误信息：%s",
                matter.getName(), matter.getMatterNo(), errorMessage);
        
        try {
            // 通知主办律师
            if (matter.getLeadLawyerId() != null) {
                notificationAppService.sendSystemNotification(
                        matter.getLeadLawyerId(),
                        "归档失败提醒",
                        content,
                        "ARCHIVE",
                        matter.getId()
                );
            }
            
            // 通知案源人
            if (matter.getOriginatorId() != null && !matter.getOriginatorId().equals(matter.getLeadLawyerId())) {
                notificationAppService.sendSystemNotification(
                        matter.getOriginatorId(),
                        "归档失败提醒",
                        content,
                        "ARCHIVE",
                        matter.getId()
                );
            }
            
            log.info("已发送归档失败通知: matterId={}", matter.getId());
        } catch (Exception notifyError) {
            log.error("发送归档失败通知时出错: matterId={}", matter.getId(), notifyError);
        }
    }

    /**
     * 从合同复制参与人到项目
     * 当基于合同创建项目时，自动将合同参与人复制为项目参与人
     */
    private void copyContractParticipantsToMatter(Long contractId, Long matterId) {
        List<ContractParticipant> contractParticipants = contractParticipantRepository.findByContractId(contractId);
        
        if (contractParticipants == null || contractParticipants.isEmpty()) {
            log.debug("合同无参与人，跳过复制: contractId={}", contractId);
            return;
        }
        
        for (ContractParticipant cp : contractParticipants) {
            // 检查是否已存在
            if (participantMapper.countByMatterIdAndUserId(matterId, cp.getUserId()) > 0) {
                log.debug("参与人已存在，跳过: matterId={}, userId={}", matterId, cp.getUserId());
                continue;
            }
            
            // 映射合同参与人角色到项目参与人角色
            String matterRole = mapContractRoleToMatterRole(cp.getRole());
            boolean isOriginator = "ORIGINATOR".equals(cp.getRole());
            
            MatterParticipant mp = MatterParticipant.builder()
                    .matterId(matterId)
                    .userId(cp.getUserId())
                    .role(matterRole)
                    .commissionRate(cp.getCommissionRate())
                    .isOriginator(isOriginator)
                    .joinDate(LocalDate.now())
                    .status("ACTIVE")
                    .remark("从合同自动复制")
                    .build();
            
            participantMapper.insert(mp);
            log.debug("复制合同参与人到项目: contractId={}, matterId={}, userId={}, role={}", 
                    contractId, matterId, cp.getUserId(), matterRole);
        }
        
        log.info("从合同复制参与人到项目完成: contractId={}, matterId={}, count={}", 
                contractId, matterId, contractParticipants.size());
    }

    /**
     * 映射合同参与人角色到项目参与人角色
     */
    private String mapContractRoleToMatterRole(String contractRole) {
        if (contractRole == null) {
            return "CO_COUNSEL";
        }
        return switch (contractRole) {
            case "LEAD" -> "LEAD";           // 承办律师 -> 主办律师
            case "CO_COUNSEL" -> "CO_COUNSEL"; // 协办律师 -> 协办律师
            case "ORIGINATOR" -> "CO_COUNSEL"; // 案源人 -> 协办律师（isOriginator标记为true）
            case "PARALEGAL" -> "PARALEGAL";   // 律师助理 -> 律师助理
            default -> "CO_COUNSEL";
        };
    }
}

