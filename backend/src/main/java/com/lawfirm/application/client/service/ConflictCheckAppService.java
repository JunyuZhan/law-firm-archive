package com.lawfirm.application.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.client.command.CreateConflictCheckCommand;
import com.lawfirm.application.client.dto.ConflictCheckDTO;
import com.lawfirm.application.client.dto.ConflictCheckItemDTO;
import com.lawfirm.application.client.dto.ConflictCheckQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.client.entity.ConflictCheck;
import com.lawfirm.domain.client.entity.ConflictCheckItem;
import com.lawfirm.application.workbench.service.ApprovalService;
import com.lawfirm.application.workbench.service.ApproverService;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.repository.ConflictCheckRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckItemMapper;
import com.lawfirm.infrastructure.persistence.mapper.ConflictCheckMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 利冲检查应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictCheckAppService {

    private final ConflictCheckRepository conflictCheckRepository;
    private final ConflictCheckMapper conflictCheckMapper;
    private final ConflictCheckItemMapper conflictCheckItemMapper;
    private final ClientRepository clientRepository;
    private final MatterRepository matterRepository;
    private final ApprovalService approvalService;
    private final ApproverService approverService;

    /**
     * 分页查询利冲检查
     */
    public PageResult<ConflictCheckDTO> listConflictChecks(ConflictCheckQueryDTO query) {
        LambdaQueryWrapper<ConflictCheck> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getCheckNo())) {
            wrapper.like(ConflictCheck::getCheckNo, query.getCheckNo());
        }
        if (query.getMatterId() != null) {
            wrapper.eq(ConflictCheck::getMatterId, query.getMatterId());
        }
        if (query.getClientId() != null) {
            wrapper.eq(ConflictCheck::getClientId, query.getClientId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ConflictCheck::getStatus, query.getStatus());
        }
        if (query.getApplicantId() != null) {
            wrapper.eq(ConflictCheck::getApplicantId, query.getApplicantId());
        }
        
        wrapper.orderByDesc(ConflictCheck::getCreatedAt);

        IPage<ConflictCheck> page = conflictCheckRepository.page(
                new Page<>(query.getPageNum(), query.getPageSize()), 
                wrapper
        );

        List<ConflictCheckDTO> records = page.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return PageResult.of(records, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 创建利冲检查
     */
    @Transactional
    public ConflictCheckDTO createConflictCheck(CreateConflictCheckCommand command) {
        // 1. 验证案件存在
        Matter matter = matterRepository.getByIdOrThrow(command.getMatterId(), "案件不存在");

        // 2. 生成检查编号
        String checkNo = generateCheckNo();

        // 3. 创建检查记录
        ConflictCheck check = ConflictCheck.builder()
                .checkNo(checkNo)
                .checkType("NEW_MATTER")
                .matterId(command.getMatterId())
                .clientId(command.getClientId() != null ? command.getClientId() : matter.getClientId())
                .clientName(matter.getName())
                .status("PENDING")
                .applicantId(SecurityUtils.getUserId())
                .remark(command.getRemark())
                .build();

        conflictCheckRepository.save(check);

        // 4. 创建检查项并执行检查
        List<ConflictCheckItem> items = new ArrayList<>();
        boolean hasConflict = false;

        for (CreateConflictCheckCommand.PartyCommand party : command.getParties()) {
            ConflictCheckItem item = ConflictCheckItem.builder()
                    .checkId(check.getId())
                    .partyName(party.getPartyName())
                    .partyType(party.getPartyType())
                    .idNumber(party.getIdNumber())
                    .hasConflict(false)
                    .build();

            // 执行冲突检查
            ConflictResult result = checkConflict(party.getPartyName(), party.getIdNumber(), command.getMatterId());
            if (result.hasConflict) {
                item.setHasConflict(true);
                item.setConflictDetail(result.detail);
                item.setRelatedMatterId(result.relatedMatterId);
                item.setRelatedClientId(result.relatedClientId);
                hasConflict = true;
            }

            conflictCheckItemMapper.insert(item);
            items.add(item);
        }

        // 5. 更新检查状态
        check.setStatus(hasConflict ? "CONFLICT" : "PASSED");
        conflictCheckRepository.updateById(check);

        // 6. 更新案件冲突状态
        matter.setConflictStatus(hasConflict ? "CONFLICT" : "PASSED");
        matterRepository.updateById(matter);

        // 7. 如果检测到冲突，创建审批记录
        if (hasConflict) {
            Long approverId = approverService.findConflictCheckApprover();
            if (approverId == null) {
                approverId = approverService.findDefaultApprover();
            }
            
            approvalService.createApproval(
                    "CONFLICT_CHECK",
                    check.getId(),
                    check.getCheckNo(),
                    "利冲检查：" + check.getClientName(),
                    approverId,
                    "HIGH",  // 冲突检查优先级高
                    "URGENT",  // 冲突检查紧急
                    null  // businessSnapshot
            );
            
            log.info("利冲检查发现冲突，已创建审批记录: {} (审批人: {})", check.getCheckNo(), approverId);
        }

        log.info("利冲检查完成: {} ({}), 结果: {}", check.getCheckNo(), 
                hasConflict ? "存在冲突" : "无冲突");
        
        ConflictCheckDTO dto = toDTO(check);
        dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
        return dto;
    }

    /**
     * 获取利冲检查详情
     */
    public ConflictCheckDTO getConflictCheckById(Long id) {
        ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");
        ConflictCheckDTO dto = toDTO(check);
        
        // 加载检查项
        List<ConflictCheckItem> items = conflictCheckItemMapper.selectByCheckId(id);
        dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
        
        return dto;
    }

    /**
     * 审核通过
     */
    @Transactional
    public void approve(Long id, String comment) {
        ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");
        
        if (!"CONFLICT".equals(check.getStatus()) && !"PENDING".equals(check.getStatus())) {
            throw new BusinessException("当前状态不允许审核");
        }

        check.setStatus("WAIVED");
        check.setReviewerId(SecurityUtils.getUserId());
        check.setReviewedAt(LocalDateTime.now());
        check.setReviewComment(comment);
        conflictCheckRepository.updateById(check);

        // 更新案件冲突状态
        if (check.getMatterId() != null) {
            Matter matter = matterRepository.findById(check.getMatterId());
            if (matter != null) {
                matter.setConflictStatus("WAIVED");
                matterRepository.updateById(matter);
            }
        }

        log.info("利冲检查审核通过: {}", check.getCheckNo());
    }

    /**
     * 申请利益冲突豁免
     */
    @Transactional
    public ConflictCheckDTO applyExemption(com.lawfirm.application.client.command.ApplyExemptionCommand command) {
        ConflictCheck check = conflictCheckRepository.getByIdOrThrow(command.getConflictCheckId(), "利冲检查不存在");
        
        // 只有发现冲突的检查才能申请豁免
        if (!"CONFLICT".equals(check.getStatus())) {
            throw new BusinessException("只有存在冲突的利冲检查才能申请豁免");
        }

        // 更新检查记录，添加豁免申请信息
        check.setStatus("EXEMPTION_PENDING"); // 豁免待审批
        check.setReviewComment(command.getExemptionReason());
        check.setRemark(command.getExemptionDescription());
        conflictCheckRepository.updateById(check);

        // 触发审批流程
        Long approverId = approverService.findDefaultApprover();
        if (approverId == null) {
            throw new BusinessException("未找到合适的审批人，请联系管理员配置审批流程");
        }

        try {
            String businessSnapshot = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(check);
            approvalService.createApproval(
                    "CONFLICT_EXEMPTION",
                    check.getId(),
                    check.getCheckNo(),
                    "利益冲突豁免申请: " + check.getCheckNo() + " - " + command.getExemptionReason(),
                    approverId,
                    "HIGH", // 高优先级
                    "URGENT", // 紧急
                    businessSnapshot
            );
        } catch (Exception e) {
            log.error("创建豁免审批失败", e);
            throw new BusinessException("创建豁免审批失败");
        }

        log.info("申请利益冲突豁免: checkNo={}, reason={}", check.getCheckNo(), command.getExemptionReason());
        return getConflictCheckById(check.getId());
    }

    /**
     * 审批豁免申请（通过）
     */
    @Transactional
    public void approveExemption(Long id, String comment) {
        ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");
        
        if (!"EXEMPTION_PENDING".equals(check.getStatus())) {
            throw new BusinessException("当前状态不允许审批豁免");
        }

        check.setStatus("WAIVED");
        check.setReviewerId(SecurityUtils.getUserId());
        check.setReviewedAt(LocalDateTime.now());
        if (StringUtils.hasText(comment)) {
            check.setReviewComment(check.getReviewComment() + "\n审批意见: " + comment);
        }
        conflictCheckRepository.updateById(check);

        // 更新案件冲突状态
        if (check.getMatterId() != null) {
            Matter matter = matterRepository.findById(check.getMatterId());
            if (matter != null) {
                matter.setConflictStatus("WAIVED");
                matterRepository.updateById(matter);
            }
        }

        log.info("利益冲突豁免审批通过: {}", check.getCheckNo());
    }

    /**
     * 审批豁免申请（拒绝）
     */
    @Transactional
    public void rejectExemption(Long id, String comment) {
        ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");
        
        if (!"EXEMPTION_PENDING".equals(check.getStatus())) {
            throw new BusinessException("当前状态不允许审批豁免");
        }

        // 拒绝后恢复为冲突状态
        check.setStatus("CONFLICT");
        check.setReviewerId(SecurityUtils.getUserId());
        check.setReviewedAt(LocalDateTime.now());
        if (StringUtils.hasText(comment)) {
            check.setReviewComment(check.getReviewComment() + "\n拒绝原因: " + comment);
        }
        conflictCheckRepository.updateById(check);

        // 更新案件冲突状态
        if (check.getMatterId() != null) {
            Matter matter = matterRepository.findById(check.getMatterId());
            if (matter != null) {
                matter.setConflictStatus("CONFLICT");
                matterRepository.updateById(matter);
            }
        }

        log.info("利益冲突豁免审批拒绝: {}", check.getCheckNo());
    }

    /**
     * 审核拒绝
     */
    @Transactional
    public void reject(Long id, String comment) {
        ConflictCheck check = conflictCheckRepository.getByIdOrThrow(id, "利冲检查不存在");
        
        if (!"CONFLICT".equals(check.getStatus()) && !"PENDING".equals(check.getStatus())) {
            throw new BusinessException("当前状态不允许审核");
        }

        check.setStatus("REJECTED");
        check.setReviewerId(SecurityUtils.getUserId());
        check.setReviewedAt(LocalDateTime.now());
        check.setReviewComment(comment);
        conflictCheckRepository.updateById(check);

        // 更新案件冲突状态
        if (check.getMatterId() != null) {
            Matter matter = matterRepository.findById(check.getMatterId());
            if (matter != null) {
                matter.setConflictStatus("REJECTED");
                matterRepository.updateById(matter);
            }
        }

        log.info("利冲检查审核拒绝: {}", check.getCheckNo());
    }

    /**
     * 执行冲突检查
     */
    private ConflictResult checkConflict(String partyName, String idNumber, Long excludeMatterId) {
        ConflictResult result = new ConflictResult();
        
        // 1. 检查是否为现有客户的对方当事人
        List<Matter> matters = matterRepository.list(
                new LambdaQueryWrapper<Matter>()
                        .ne(Matter::getId, excludeMatterId)
                        .and(w -> w
                                .like(Matter::getOpposingParty, partyName)
                                .or()
                                .like(Matter::getName, partyName)
                        )
        );

        if (!matters.isEmpty()) {
            result.hasConflict = true;
            result.detail = "该当事人与现有案件存在关联";
            result.relatedMatterId = matters.get(0).getId();
        }

        // 2. 检查是否为现有客户
        if (StringUtils.hasText(idNumber)) {
            List<Client> clients = clientRepository.list(
                    new LambdaQueryWrapper<Client>()
                            .eq(Client::getCreditCode, idNumber)
                            .or()
                            .eq(Client::getIdCard, idNumber)
            );
            
            if (!clients.isEmpty()) {
                // 检查该客户是否有对立案件
                Client client = clients.get(0);
                List<Matter> clientMatters = matterRepository.list(
                        new LambdaQueryWrapper<Matter>()
                                .eq(Matter::getClientId, client.getId())
                                .ne(Matter::getId, excludeMatterId)
                );
                
                if (!clientMatters.isEmpty()) {
                    result.hasConflict = true;
                    result.detail = "该当事人为现有客户，存在潜在利益冲突";
                    result.relatedClientId = client.getId();
                    result.relatedMatterId = clientMatters.get(0).getId();
                }
            }
        }

        return result;
    }

    /**
     * 生成检查编号
     */
    private String generateCheckNo() {
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CC" + System.currentTimeMillis() % 100000 + random;
    }

    /**
     * 获取状态名称
     */
    private String getStatusName(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING" -> "待检查";
            case "CHECKING" -> "检查中";
            case "PASSED" -> "已通过";
            case "CONFLICT" -> "存在冲突";
            case "WAIVED" -> "已豁免";
            case "EXEMPTION_PENDING" -> "豁免待审批";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    /**
     * 获取检查类型名称
     */
    private String getCheckTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "NEW_CLIENT" -> "新客户";
            case "NEW_MATTER" -> "新案件";
            case "MANUAL" -> "手动检查";
            default -> type;
        };
    }

    /**
     * 获取当事人类型名称
     */
    private String getPartyTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "CLIENT" -> "委托人";
            case "OPPOSING" -> "对方当事人";
            case "RELATED" -> "关联方";
            default -> type;
        };
    }

    /**
     * Entity 转 DTO
     */
    private ConflictCheckDTO toDTO(ConflictCheck check) {
        ConflictCheckDTO dto = new ConflictCheckDTO();
        dto.setId(check.getId());
        dto.setCheckNo(check.getCheckNo());
        dto.setMatterId(check.getMatterId());
        dto.setClientId(check.getClientId());
        dto.setClientName(check.getClientName());
        dto.setCheckType(check.getCheckType());
        dto.setCheckTypeName(getCheckTypeName(check.getCheckType()));
        dto.setStatus(check.getStatus());
        dto.setStatusName(getStatusName(check.getStatus()));
        dto.setApplicantId(check.getApplicantId());
        dto.setReviewerId(check.getReviewerId());
        dto.setReviewTime(check.getReviewedAt());
        dto.setReviewComment(check.getReviewComment());
        dto.setRemark(check.getRemark());
        dto.setCreatedAt(check.getCreatedAt());
        dto.setUpdatedAt(check.getUpdatedAt());
        return dto;
    }

    /**
     * Item Entity 转 DTO
     */
    private ConflictCheckItemDTO toItemDTO(ConflictCheckItem item) {
        ConflictCheckItemDTO dto = new ConflictCheckItemDTO();
        dto.setId(item.getId());
        dto.setCheckId(item.getCheckId());
        dto.setPartyName(item.getPartyName());
        dto.setPartyType(item.getPartyType());
        dto.setPartyTypeName(getPartyTypeName(item.getPartyType()));
        dto.setIdNumber(item.getIdNumber());
        dto.setHasConflict(item.getHasConflict());
        dto.setConflictDetail(item.getConflictDetail());
        dto.setRelatedMatterId(item.getRelatedMatterId());
        dto.setRelatedClientId(item.getRelatedClientId());
        return dto;
    }

    /**
     * 冲突检查结果
     */
    private static class ConflictResult {
        boolean hasConflict = false;
        String detail;
        Long relatedMatterId;
        Long relatedClientId;
    }
}

