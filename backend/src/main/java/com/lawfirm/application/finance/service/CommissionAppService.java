package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateCommissionRuleCommand;
import com.lawfirm.application.finance.command.ManualCalculateCommissionCommand;
import com.lawfirm.application.finance.command.UpdateCommissionRuleCommand;
import com.lawfirm.application.finance.dto.*;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.CommissionDetail;
import com.lawfirm.domain.finance.entity.CommissionRule;
import com.lawfirm.infrastructure.persistence.mapper.CommissionDetailMapper;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.domain.finance.entity.ContractParticipant;
import com.lawfirm.domain.finance.entity.Fee;
import com.lawfirm.domain.finance.entity.Payment;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.CommissionRuleRepository;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import com.lawfirm.domain.finance.repository.ContractRepository;
import com.lawfirm.domain.finance.repository.FeeRepository;
import com.lawfirm.domain.finance.repository.PaymentRepository;
import com.lawfirm.domain.client.repository.ClientRepository;
import com.lawfirm.domain.client.entity.Client;
import com.lawfirm.domain.matter.repository.MatterRepository;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提成应用服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommissionAppService {

    private final CommissionRuleRepository commissionRuleRepository;
    private final CommissionRepository commissionRepository;
    private final CommissionDetailMapper commissionDetailMapper;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final FeeRepository feeRepository;
    private final ContractRepository contractRepository;
    private final ContractParticipantRepository contractParticipantRepository;
    private final ClientRepository clientRepository;
    private final MatterRepository matterRepository;

    // ========== 提成规则管理 ==========

    /**
     * 创建提成规则（仅admin/director）
     */
    @Transactional(rollbackFor = Exception.class)
    public CommissionRuleDTO createCommissionRule(CreateCommissionRuleCommand command) {
        // 权限检查：只有admin和director可以创建规则
        checkRulePermission();

        // 检查规则编码是否已存在
        if (commissionRuleRepository.findByRuleCode(command.getRuleCode()).isPresent()) {
            throw new BusinessException("规则编码已存在");
        }

        // 比例允许为0（表示不参与分配），不强制总和=100%
        CommissionRule rule = CommissionRule.builder()
                .ruleCode(command.getRuleCode())
                .ruleName(command.getRuleName())
                .ruleType(command.getRuleType())
                .firmRate(command.getFirmRate() != null ? command.getFirmRate() : BigDecimal.ZERO)
                .leadLawyerRate(command.getLeadLawyerRate() != null ? command.getLeadLawyerRate() : BigDecimal.ZERO)
                .assistLawyerRate(command.getAssistLawyerRate() != null ? command.getAssistLawyerRate() : BigDecimal.ZERO)
                .supportStaffRate(command.getSupportStaffRate() != null ? command.getSupportStaffRate() : BigDecimal.ZERO)
                .originatorRate(command.getOriginatorRate() != null ? command.getOriginatorRate() : BigDecimal.ZERO)
                .allowModify(command.getAllowModify() != null ? command.getAllowModify() : true)
                .description(command.getDescription())
                .isDefault(command.getIsDefault() != null ? command.getIsDefault() : false)
                .active(command.getActive() != null ? command.getActive() : true)
                .createdBy(SecurityUtils.getUserId())
                .build();

        // 如果设为默认，取消其他规则的默认状态
        if (Boolean.TRUE.equals(rule.getIsDefault())) {
            commissionRuleRepository.clearDefault();
        }

        commissionRuleRepository.getBaseMapper().insert(rule);

        return toDTO(rule);
    }

    /**
     * 更新提成规则（仅admin/director）
     */
    @Transactional(rollbackFor = Exception.class)
    public CommissionRuleDTO updateCommissionRule(Long id, UpdateCommissionRuleCommand command) {
        checkRulePermission();

        CommissionRule rule = commissionRuleRepository.findById(id);
        if (rule == null) {
            throw new BusinessException("提成规则不存在");
        }

        if (command.getRuleName() != null) {
            rule.setRuleName(command.getRuleName());
        }
        if (command.getRuleType() != null) {
            rule.setRuleType(command.getRuleType());
        }
        if (command.getFirmRate() != null) {
            rule.setFirmRate(command.getFirmRate());
        }
        if (command.getLeadLawyerRate() != null) {
            rule.setLeadLawyerRate(command.getLeadLawyerRate());
        }
        if (command.getAssistLawyerRate() != null) {
            rule.setAssistLawyerRate(command.getAssistLawyerRate());
        }
        if (command.getSupportStaffRate() != null) {
            rule.setSupportStaffRate(command.getSupportStaffRate());
        }
        if (command.getOriginatorRate() != null) {
            rule.setOriginatorRate(command.getOriginatorRate());
        }
        if (command.getAllowModify() != null) {
            rule.setAllowModify(command.getAllowModify());
        }
        if (command.getDescription() != null) {
            rule.setDescription(command.getDescription());
        }
        if (command.getIsDefault() != null) {
            // 如果设为默认，取消其他规则的默认状态
            if (Boolean.TRUE.equals(command.getIsDefault()) && !Boolean.TRUE.equals(rule.getIsDefault())) {
                commissionRuleRepository.clearDefault();
            }
            rule.setIsDefault(command.getIsDefault());
        }
        if (command.getActive() != null) {
            rule.setActive(command.getActive());
        }

        // 比例允许为0，不强制总和=100%

        rule.setUpdatedBy(SecurityUtils.getUserId());
        commissionRuleRepository.getBaseMapper().updateById(rule);

        return toDTO(rule);
    }

    /**
     * 删除提成规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommissionRule(Long id) {
        checkRulePermission();

        CommissionRule rule = commissionRuleRepository.findById(id);
        if (rule == null) {
            throw new BusinessException("提成规则不存在");
        }

        if (Boolean.TRUE.equals(rule.getIsDefault())) {
            throw new BusinessException("不能删除默认规则");
        }

        commissionRuleRepository.softDelete(id);
    }

    /**
     * 设为默认规则
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultRule(Long id) {
        checkRulePermission();

        CommissionRule rule = commissionRuleRepository.findById(id);
        if (rule == null) {
            throw new BusinessException("提成规则不存在");
        }

        commissionRuleRepository.setDefault(id);
        log.info("设置默认提成规则: id={}", id);
    }

    /**
     * 切换规则启用状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleRule(Long id) {
        checkRulePermission();

        CommissionRule rule = commissionRuleRepository.findById(id);
        if (rule == null) {
            throw new BusinessException("提成规则不存在");
        }

        if (Boolean.TRUE.equals(rule.getIsDefault()) && Boolean.TRUE.equals(rule.getActive())) {
            throw new BusinessException("不能停用默认规则");
        }

        rule.setActive(!Boolean.TRUE.equals(rule.getActive()));
        rule.setUpdatedBy(SecurityUtils.getUserId());
        commissionRuleRepository.getBaseMapper().updateById(rule);
        log.info("切换规则状态: id={}, active={}", id, rule.getActive());
    }

    /**
     * 获取提成规则详情
     */
    public CommissionRuleDTO getCommissionRule(Long id) {
        CommissionRule rule = commissionRuleRepository.findById(id);
        if (rule == null) {
            throw new BusinessException("提成规则不存在");
        }
        return toDTO(rule);
    }

    /**
     * 查询提成规则列表
     */
    public List<CommissionRuleDTO> listCommissionRules() {
        List<CommissionRule> rules = commissionRuleRepository.findActiveRules();
        return rules.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ========== 提成计算 ==========

    /**
     * 手动计算提成（财务用户手动计算）
     * 根据合同参与人的提成比例，财务可以手动修改提成金额
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CommissionDTO> manualCalculateCommission(ManualCalculateCommissionCommand command) {
        // 获取收款记录
        Payment payment = paymentRepository.getByIdOrThrow(command.getPaymentId(), "收款记录不存在");
        if (!"CONFIRMED".equals(payment.getStatus())) {
            throw new BusinessException("只有已确认的收款才能计算提成");
        }

        // 获取收费记录
        Fee fee = feeRepository.getByIdOrThrow(payment.getFeeId(), "收费记录不存在");
        
        // 获取合同
        Contract contract = null;
        if (fee.getContractId() != null) {
            contract = contractRepository.findById(fee.getContractId());
        }
        
        if (contract == null) {
            throw new BusinessException("该收款记录没有关联合同，无法计算提成");
        }

        // 检查是否已有提成记录
        List<Commission> existingCommissions = commissionRepository.findByPaymentId(payment.getId());
        if (!existingCommissions.isEmpty()) {
            throw new BusinessException("该收款记录已存在提成记录，请先删除后再重新计算");
        }

        // 创建提成记录
        List<CommissionDTO> result = new java.util.ArrayList<>();
        BigDecimal paymentAmount = payment.getAmount();
        
        for (ManualCalculateCommissionCommand.ParticipantCommission pc : command.getParticipants()) {
            // 获取参与人信息
            ContractParticipant participant = contractParticipantRepository.getByIdOrThrow(pc.getParticipantId(), "参与人不存在");
            
            if (!participant.getContractId().equals(contract.getId())) {
                throw new BusinessException("参与人不属于该合同");
            }

            // 计算提成金额（如果未指定，则根据比例计算）
            BigDecimal commissionAmount = pc.getCommissionAmount();
            if (commissionAmount == null) {
                BigDecimal rate = pc.getCommissionRate() != null ? pc.getCommissionRate() : participant.getCommissionRate();
                if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
                    log.warn("参与人提成比例为0，跳过: participantId={}", pc.getParticipantId());
                    continue;
                }
                commissionAmount = paymentAmount.multiply(rate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            }

            // 创建提成记录
            Commission commission = Commission.builder()
                    .commissionNo(generateCommissionNo())
                    .paymentId(payment.getId())
                    .contractId(contract.getId())
                    .matterId(fee.getMatterId())
                    .ruleId(contract.getCommissionRuleId())
                    .grossAmount(paymentAmount)
                    .netAmount(paymentAmount) // 净收入暂时等于毛收入，财务后续可调整
                    .distributionRatio(pc.getCommissionRate() != null ? pc.getCommissionRate() : participant.getCommissionRate())
                    .commissionRate(pc.getCommissionRate() != null ? pc.getCommissionRate() : participant.getCommissionRate())
                    .commissionAmount(commissionAmount)
                    .status("CALCULATED") // 已计算，待审批
                    .remark(pc.getRemark())
                    .createdBy(SecurityUtils.getUserId())
                    .build();

            commissionRepository.getBaseMapper().insert(commission);
            
            // 创建提成明细记录（关联到具体用户）
            String userName = participant.getUserId() != null ? 
                    userRepository.findById(participant.getUserId()) != null ? 
                            userRepository.findById(participant.getUserId()).getRealName() : "未知" : "未知";
            
            CommissionDetail detail = CommissionDetail.builder()
                    .commissionId(commission.getId())
                    .userId(participant.getUserId())
                    .userName(userName)
                    .roleInMatter(participant.getRole())
                    .allocationRate(pc.getCommissionRate() != null ? pc.getCommissionRate() : participant.getCommissionRate())
                    .commissionAmount(commissionAmount)
                    .netAmount(commissionAmount) // 净提成暂时等于提成金额
                    .build();
            
            commissionDetailMapper.insert(detail);
            
            result.add(toCommissionDTO(commission));
            
            log.info("手动计算提成成功: paymentId={}, participantId={}, userId={}, commissionAmount={}", 
                    payment.getId(), pc.getParticipantId(), participant.getUserId(), commissionAmount);
        }

        return result;
    }

    /**
     * 生成提成编号
     */
    private String generateCommissionNo() {
        return "COM" + System.currentTimeMillis();
    }

    // ========== 提成查询 ==========

    /**
     * 查询提成记录列表
     * 数据权限：ADMIN/DIRECTOR/TEAM_LEADER/FINANCE 可看全部，其他人只能看自己的
     */
    public PageResult<CommissionDTO> listCommissions(CommissionQueryDTO query) {
        // 数据权限：普通律师只能看自己的提成（通过 commission_detail 表关联）
        Long currentUserId = SecurityUtils.getUserId();
        if (!canViewAllCommissions()) {
            // 如果查询指定了 userId，且不是当前用户，则无权限
            if (query.getUserId() != null && !query.getUserId().equals(currentUserId)) {
                throw new BusinessException("无权查看他人的提成记录");
            }
            // 只查询当前用户的提成（通过 commission_detail 表）
            query.setUserId(currentUserId);
        }
        
        // 如果指定了 userId，需要通过 commission_detail 表关联查询
        if (query.getUserId() != null) {
            // 使用自定义查询方法（通过 commission_detail 表）
            int offset = (query.getPageNum() - 1) * query.getPageSize();
            List<Commission> commissions = commissionRepository.findByUserId(query.getUserId(), offset, query.getPageSize());
            
            // 应用其他过滤条件
            List<Commission> filtered = commissions.stream()
                    .filter(c -> {
                        if (query.getPaymentId() != null && !c.getPaymentId().equals(query.getPaymentId())) {
                            return false;
                        }
                        if (query.getMatterId() != null && !c.getMatterId().equals(query.getMatterId())) {
                            return false;
                        }
                        if (query.getStatus() != null && !c.getStatus().equals(query.getStatus())) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
            
            List<CommissionDTO> dtos = filtered.stream()
                    .map(this::toCommissionDTO)
                    .collect(Collectors.toList());
            
            // 注意：这里无法准确获取总数，暂时使用当前页的数量
            return PageResult.of(dtos, (long) filtered.size(), query.getPageNum(), query.getPageSize());
        }
        
        // 如果没有指定 userId，使用常规查询
        Page<Commission> page = new Page<>(query.getPageNum(), query.getPageSize());
        
        LambdaQueryWrapper<Commission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Commission::getDeleted, false);
        
        if (query.getPaymentId() != null) {
            wrapper.eq(Commission::getPaymentId, query.getPaymentId());
        }
        if (query.getMatterId() != null) {
            wrapper.eq(Commission::getMatterId, query.getMatterId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Commission::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(Commission::getCreatedAt);
        
        Page<Commission> result = commissionRepository.page(page, wrapper);
        
        List<CommissionDTO> dtos = result.getRecords().stream()
                .map(this::toCommissionDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(dtos, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取提成记录详情
     */
    public CommissionDTO getCommission(Long id) {
        Commission commission = commissionRepository.findById(id);
        if (commission == null) {
            throw new BusinessException("提成记录不存在");
        }
        
        // 权限检查：普通律师只能查看自己的提成（通过 commission_detail 表检查）
        Long currentUserId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(currentUserId);
        if (!roleCodes.contains("ADMIN") && !roleCodes.contains("DIRECTOR") && !roleCodes.contains("TEAM_LEADER")) {
            // 检查 commission_detail 表中是否有该用户的记录
            int count = commissionRepository.getBaseMapper().countByCommissionIdAndUserId(id, currentUserId);
            if (count == 0) {
                throw new BusinessException("无权查看该提成记录");
            }
        }
        
        return toCommissionDTO(commission);
    }

    /**
     * 查询用户的提成总额
     */
    public BigDecimal getUserTotalCommission(Long userId) {
        // 权限检查：普通律师只能查看自己的提成总额
        Long currentUserId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(currentUserId);
        if (!roleCodes.contains("ADMIN") && !roleCodes.contains("DIRECTOR") && !roleCodes.contains("TEAM_LEADER")) {
            if (!userId.equals(currentUserId)) {
                throw new BusinessException("无权查看他人的提成总额");
            }
        }
        
        return commissionRepository.sumCommissionByUserId(userId);
    }

    // ========== 提成汇总查看（M4-037，P1） ==========

    /**
     * 获取全所提成汇总（仅管理层）
     */
    public Map<String, Object> getCommissionSummary(String startDate, String endDate) {
        // 权限检查：只有管理层可以查看汇总
        checkManagementPermission();
        
        // 查询汇总数据
        BigDecimal totalCommission = commissionRepository.sumTotalCommission(startDate, endDate);
        BigDecimal approvedCommission = commissionRepository.sumCommissionByStatus("APPROVED", startDate, endDate);
        BigDecimal paidCommission = commissionRepository.sumCommissionByStatus("PAID", startDate, endDate);
        Long totalCount = commissionRepository.countCommissions(startDate, endDate);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCommission", totalCommission);
        summary.put("approvedCommission", approvedCommission);
        summary.put("paidCommission", paidCommission);
        summary.put("pendingCommission", totalCommission.subtract(approvedCommission));
        summary.put("totalCount", totalCount);
        
        // 按用户汇总
        List<Map<String, Object>> userSummary = commissionRepository.sumCommissionByUser(startDate, endDate);
        summary.put("userSummary", userSummary);
        
        return summary;
    }

    // ========== 提成审批（M4-038，P1） ==========

    /**
     * 审批提成（仅director）
     */
    @Transactional(rollbackFor = Exception.class)
    public CommissionDTO approveCommission(Long id, Boolean approved, String comment) {
        // 权限检查：只有director可以审批
        checkDirectorPermission();
        
        Commission commission = commissionRepository.findById(id);
        if (commission == null) {
            throw new BusinessException("提成记录不存在");
        }
        
        if (!"CALCULATED".equals(commission.getStatus())) {
            throw new BusinessException("只有已计算的提成才能审批");
        }
        
        if (Boolean.TRUE.equals(approved)) {
            commission.setStatus("APPROVED");
            commission.setApprovedBy(SecurityUtils.getUserId());
            commission.setApprovedAt(LocalDateTime.now());
            if (comment != null) {
                commission.setRemark((commission.getRemark() != null ? commission.getRemark() + "\n" : "") + 
                        "审批通过: " + comment);
            }
        } else {
            commission.setStatus("CALCULATED");
            if (comment != null) {
                commission.setRemark((commission.getRemark() != null ? commission.getRemark() + "\n" : "") + 
                        "审批驳回: " + comment);
            }
        }
        
        commissionRepository.getBaseMapper().updateById(commission);
        log.info("提成审批完成: id={}, approved={}", id, approved);
        return toCommissionDTO(commission);
    }

    /**
     * 批量审批提成
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchApproveCommissions(List<Long> ids, Boolean approved, String comment) {
        checkDirectorPermission();
        
        for (Long id : ids) {
            approveCommission(id, approved, comment);
        }
        log.info("批量审批提成完成: count={}, approved={}", ids.size(), approved);
    }

    // ========== 提成发放（M4-039，P1） ==========

    /**
     * 确认提成已发放（仅finance）
     */
    @Transactional(rollbackFor = Exception.class)
    public CommissionDTO issueCommission(Long id) {
        // 权限检查：只有财务人员可以确认发放
        checkFinancePermission();
        
        Commission commission = commissionRepository.findById(id);
        if (commission == null) {
            throw new BusinessException("提成记录不存在");
        }
        
        if (!"APPROVED".equals(commission.getStatus())) {
            throw new BusinessException("只有已审批的提成才能发放");
        }
        
        commission.setStatus("PAID");
        commission.setPaidAt(LocalDateTime.now());
        commissionRepository.getBaseMapper().updateById(commission);
        
        log.info("提成发放确认完成: id={}, amount={}", id, commission.getCommissionAmount());
        return toCommissionDTO(commission);
    }

    /**
     * 批量确认提成发放
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchIssueCommissions(List<Long> ids) {
        checkFinancePermission();
        
        for (Long id : ids) {
            issueCommission(id);
        }
        log.info("批量发放提成完成: count={}", ids.size());
    }

    // ========== 提成报表（M4-040，P1） ==========

    /**
     * 生成提成报表数据
     */
    public List<Map<String, Object>> getCommissionReportData(String startDate, String endDate, Long userId) {
        // 权限检查
        Long currentUserId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(currentUserId);
        if (!roleCodes.contains("ADMIN") && !roleCodes.contains("DIRECTOR") && !roleCodes.contains("TEAM_LEADER") 
                && !roleCodes.contains("FINANCE")) {
            if (userId == null || !userId.equals(currentUserId)) {
                throw new BusinessException("无权查看提成报表");
            }
        }
        
        return commissionRepository.queryCommissionReportData(startDate, endDate, userId);
    }

    // ========== 权限检查 ==========

    /**
     * 检查是否有规则管理权限（仅ADMIN/DIRECTOR）
     */
    private void checkRulePermission() {
        Long userId = SecurityUtils.getUserId();
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("ADMIN") && !roleCodes.contains("DIRECTOR")) {
            throw new BusinessException("只有系统管理员和主任可以管理提成规则");
        }
    }

    /**
     * 检查是否有管理层权限（ADMIN/DIRECTOR/TEAM_LEADER）
     */
    private void checkManagementPermission() {
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("ADMIN") && !roleCodes.contains("DIRECTOR") && !roleCodes.contains("TEAM_LEADER")) {
            throw new BusinessException("只有管理层可以查看提成汇总");
        }
    }

    /**
     * 检查是否有提成审批权限（ADMIN/DIRECTOR）
     */
    private void checkDirectorPermission() {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("DIRECTOR")) {
            throw new BusinessException("只有管理员和主任可以审批提成");
        }
    }

    /**
     * 检查是否有财务权限（ADMIN/FINANCE角色）
     */
    private void checkFinancePermission() {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("FINANCE")) {
            throw new BusinessException("只有财务人员可以确认提成发放");
        }
    }
    
    /**
     * 检查是否可以查看所有提成记录（ADMIN/DIRECTOR/TEAM_LEADER/FINANCE）
     */
    private boolean canViewAllCommissions() {
        if (SecurityUtils.isAdmin()) {
            return true;
        }
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        return roleCodes.contains("DIRECTOR") || roleCodes.contains("TEAM_LEADER") || roleCodes.contains("FINANCE");
    }

    // ========== DTO转换 ==========

    private CommissionRuleDTO toDTO(CommissionRule rule) {
        CommissionRuleDTO dto = new CommissionRuleDTO();
        BeanUtils.copyProperties(rule, dto);
        return dto;
    }

    /**
     * 获取待计算提成的收款记录列表
     * 返回所有已确认但还没有生成提成记录的收款记录
     */
    public List<PaymentDTO> getPendingCommissionPayments() {
        // 查询所有已确认的收款记录
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getStatus, "CONFIRMED")
                .eq(Payment::getDeleted, false)
                .isNotNull(Payment::getContractId) // 必须有合同才能计算提成
                .orderByDesc(Payment::getPaymentDate);
        List<Payment> confirmedPayments = paymentRepository.list(paymentWrapper);
        
        // 查询所有已有提成记录的 paymentId
        LambdaQueryWrapper<Commission> commissionWrapper = new LambdaQueryWrapper<>();
        commissionWrapper.eq(Commission::getDeleted, false)
                .select(Commission::getPaymentId);
        List<Long> paymentIdsWithCommission = commissionRepository.list(commissionWrapper)
                .stream()
                .map(Commission::getPaymentId)
                .distinct()
                .collect(Collectors.toList());
        
        // 过滤掉已有提成记录的收款
        List<Payment> pendingPayments = confirmedPayments.stream()
                .filter(p -> !paymentIdsWithCommission.contains(p.getId()))
                .collect(Collectors.toList());
        
        // 转换为DTO并填充关联信息
        return pendingPayments.stream().map(payment -> {
            PaymentDTO dto = new PaymentDTO();
            BeanUtils.copyProperties(payment, dto);
            
            // 填充客户信息
            if (payment.getClientId() != null) {
                Client client = clientRepository.findById(payment.getClientId());
                if (client != null) {
                    dto.setClientId(client.getId());
                    dto.setClientName(client.getName());
                }
            }
            
            // 填充项目信息
            if (payment.getMatterId() != null) {
                Matter matter = matterRepository.findById(payment.getMatterId());
                if (matter != null) {
                    dto.setMatterId(matter.getId());
                    dto.setMatterName(matter.getName());
                }
            } else if (payment.getFeeId() != null) {
                // 如果收款记录没有项目ID，尝试从收费记录获取
                Fee fee = feeRepository.findById(payment.getFeeId());
                if (fee != null && fee.getMatterId() != null) {
                    Matter matter = matterRepository.findById(fee.getMatterId());
                    if (matter != null) {
                        dto.setMatterId(matter.getId());
                        dto.setMatterName(matter.getName());
                    }
                }
            }
            
            // 设置状态名称
            if ("CONFIRMED".equals(payment.getStatus())) {
                dto.setStatusName("已确认");
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    private CommissionDTO toCommissionDTO(Commission commission) {
        CommissionDTO dto = new CommissionDTO();
        BeanUtils.copyProperties(commission, dto);
        
        // 注意：Commission 表没有 userId 字段，提成是通过 commission_detail 表分配给用户的
        // 如果需要显示用户信息，应该从 commission_detail 表中查询
        
        return dto;
    }
}
