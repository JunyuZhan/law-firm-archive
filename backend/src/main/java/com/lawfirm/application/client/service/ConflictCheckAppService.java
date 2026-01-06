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
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import com.lawfirm.domain.system.entity.User;
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
    private final UserMapper userMapper;

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
     * 快速利冲检索（不创建记录，仅检查是否存在冲突）
     * 用于新增客户时的实时检查
     * 
     * @param clientName 客户名称
     * @param opposingParty 对方当事人
     * @return 检查结果
     */
    public QuickConflictCheckResult quickConflictCheck(String clientName, String opposingParty) {
        // 核心逻辑：检查对方当事人是否是我们的现有客户
        List<Client> conflictClients = clientRepository.list(
                new LambdaQueryWrapper<Client>()
                        .like(Client::getName, opposingParty)
        );
        
        if (!conflictClients.isEmpty()) {
            Client conflictClient = conflictClients.get(0);
            String detail = String.format(
                    "对方当事人【%s】是本所现有客户（客户编号：%s）。根据律师执业规范，不能同时代理利益冲突双方。",
                    opposingParty, conflictClient.getClientNo()
            );
            return new QuickConflictCheckResult(true, detail);
        }
        
        return new QuickConflictCheckResult(false, null);
    }

    /**
     * 快速利冲检查结果
     */
    public record QuickConflictCheckResult(boolean hasConflict, String conflictDetail) {}

    /**
     * 手动申请利冲审查（简化版，不需要关联案件）
     */
    @Transactional
    public ConflictCheckDTO applyConflictCheck(String clientName, String opposingParty, 
            String matterName, String checkType, String remark) {
        // 1. 生成检查编号
        String checkNo = generateCheckNo();

        // 2. 创建检查记录
        ConflictCheck check = ConflictCheck.builder()
                .checkNo(checkNo)
                .checkType(checkType != null ? checkType : "MANUAL")
                .clientName(clientName)
                .opposingParty(opposingParty)
                .status("PENDING")
                .applicantId(SecurityUtils.getUserId())
                .remark(remark)
                .build();

        conflictCheckRepository.save(check);

        // 3. 创建检查项
        List<ConflictCheckItem> items = new ArrayList<>();
        boolean hasConflict = false;

        // ===== 核心利冲逻辑 =====
        // 规则1：检查【对方当事人】是否是我们的现有客户（最重要！）
        //        如果对方当事人是我们客户，则存在利益冲突
        ConflictCheckItem opposingItem = ConflictCheckItem.builder()
                .checkId(check.getId())
                .partyName(opposingParty)
                .partyType("OPPOSING")
                .hasConflict(false)
                .build();
        ConflictResult opposingResult = checkOpposingPartyConflict(opposingParty);
        if (opposingResult.hasConflict) {
            opposingItem.setHasConflict(true);
            opposingItem.setConflictDetail(opposingResult.detail);
            opposingItem.setRelatedMatterId(opposingResult.relatedMatterId);
            opposingItem.setRelatedClientId(opposingResult.relatedClientId);
            hasConflict = true;
        }
        conflictCheckItemMapper.insert(opposingItem);
        items.add(opposingItem);

        // 规则2：检查【客户】是否曾作为其他案件的对方当事人
        //        如果我们之前代理别人告过这个客户，可能存在冲突
        ConflictCheckItem clientItem = ConflictCheckItem.builder()
                .checkId(check.getId())
                .partyName(clientName)
                .partyType("CLIENT")
                .hasConflict(false)
                .build();
        ConflictResult clientResult = checkClientConflict(clientName);
        if (clientResult.hasConflict) {
            clientItem.setHasConflict(true);
            clientItem.setConflictDetail(clientResult.detail);
            clientItem.setRelatedMatterId(clientResult.relatedMatterId);
            clientItem.setRelatedClientId(clientResult.relatedClientId);
            hasConflict = true;
        }
        conflictCheckItemMapper.insert(clientItem);
        items.add(clientItem);

        // 4. 更新检查状态
        check.setStatus(hasConflict ? "CONFLICT" : "PASSED");
        conflictCheckRepository.updateById(check);

        // 5. 如果检测到冲突，创建审批记录
        if (hasConflict) {
            Long approverId = approverService.findConflictCheckApprover();
            if (approverId == null) {
                approverId = approverService.findDefaultApprover();
            }
            
            approvalService.createApproval(
                    "CONFLICT_CHECK",
                    check.getId(),
                    check.getCheckNo(),
                    "利冲检查：" + check.getClientName() + " vs " + opposingParty,
                    approverId,
                    "HIGH",
                    "URGENT",
                    null
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
     * 【核心利冲规则1】检查对方当事人是否是我们的现有客户
     * 
     * 如果对方当事人是我们的客户，则存在利益冲突！
     * 例如：张三委托我们告李四，但李四也是我们的客户 → 冲突
     */
    private ConflictResult checkOpposingPartyConflict(String opposingParty) {
        ConflictResult result = new ConflictResult();
        
        // 核心检查：对方当事人是否是我们的现有客户？
        List<Client> clients = clientRepository.list(
                new LambdaQueryWrapper<Client>()
                        .like(Client::getName, opposingParty)
        );
        
        if (!clients.isEmpty()) {
            Client client = clients.get(0);
            result.hasConflict = true;
            result.detail = "【利益冲突】对方当事人「" + opposingParty + "」是本所现有客户，不能同时代理双方";
            result.relatedClientId = client.getId();
            return result;
        }
        
        // 次要检查：对方当事人是否曾作为我方客户出现在其他案件中
        List<Matter> mattersAsClient = matterRepository.list(
                new LambdaQueryWrapper<Matter>()
                        .exists("SELECT 1 FROM crm_client c WHERE c.name LIKE '%" + opposingParty + "%' AND c.id = matter.client_id")
        );
        // 简化：直接通过客户名称检查
        
        return result;
    }

    /**
     * 【核心利冲规则2】检查客户是否曾作为其他案件的对方当事人
     * 
     * 如果我们之前代理别人告过这个客户，可能存在冲突
     * 例如：我们之前代理王五告过张三，现在张三要委托我们 → 潜在冲突
     */
    private ConflictResult checkClientConflict(String clientName) {
        ConflictResult result = new ConflictResult();
        
        // 检查客户名称是否曾作为其他案件的对方当事人
        List<Matter> mattersAsOpposing = matterRepository.list(
                new LambdaQueryWrapper<Matter>()
                        .like(Matter::getOpposingParty, clientName)
        );
        
        if (!mattersAsOpposing.isEmpty()) {
            Matter matter = mattersAsOpposing.get(0);
            result.hasConflict = true;
            result.detail = "【潜在冲突】客户「" + clientName + "」曾在案件「" + matter.getName() + "」中作为对方当事人";
            result.relatedMatterId = matter.getId();
        }
        
        return result;
    }

    /**
     * 简化版冲突检查（保留兼容性，供其他地方调用）
     */
    private ConflictResult checkConflictSimple(String partyName, String idNumber) {
        ConflictResult result = new ConflictResult();
        
        // 检查是否为现有客户
        List<Client> clients = clientRepository.list(
                new LambdaQueryWrapper<Client>()
                        .like(Client::getName, partyName)
        );
        
        if (!clients.isEmpty()) {
            Client client = clients.get(0);
            result.hasConflict = true;
            result.detail = "该当事人为现有客户";
            result.relatedClientId = client.getId();
        }

        return result;
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
        dto.setOpposingParty(check.getOpposingParty());
        dto.setCheckType(check.getCheckType());
        dto.setCheckTypeName(getCheckTypeName(check.getCheckType()));
        dto.setStatus(check.getStatus());
        dto.setStatusName(getStatusName(check.getStatus()));
        dto.setApplicantId(check.getApplicantId());
        dto.setApplyTime(check.getCreatedAt()); // 申请时间 = 创建时间
        dto.setReviewerId(check.getReviewerId());
        dto.setReviewTime(check.getReviewedAt());
        dto.setReviewComment(check.getReviewComment());
        dto.setRemark(check.getRemark());
        dto.setCreatedAt(check.getCreatedAt());
        dto.setUpdatedAt(check.getUpdatedAt());
        
        // 查询申请人姓名
        if (check.getApplicantId() != null) {
            User applicant = userMapper.selectById(check.getApplicantId());
            if (applicant != null) {
                dto.setApplicantName(applicant.getRealName());
            }
        }
        
        // 查询审核人姓名
        if (check.getReviewerId() != null) {
            User reviewer = userMapper.selectById(check.getReviewerId());
            if (reviewer != null) {
                dto.setReviewerName(reviewer.getRealName());
            }
        }
        
        // 查询项目名称
        if (check.getMatterId() != null) {
            Matter matter = matterRepository.findById(check.getMatterId());
            if (matter != null) {
                dto.setMatterName(matter.getName());
            }
        }
        
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

