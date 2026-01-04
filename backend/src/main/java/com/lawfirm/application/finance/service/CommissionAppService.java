package com.lawfirm.application.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.finance.command.CreateCommissionRuleCommand;
import com.lawfirm.application.finance.command.UpdateCommissionRuleCommand;
import com.lawfirm.application.finance.dto.*;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.entity.Commission;
import com.lawfirm.domain.finance.entity.CommissionRule;
import com.lawfirm.domain.finance.repository.CommissionRepository;
import com.lawfirm.domain.finance.repository.CommissionRuleRepository;
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
    private final CommissionCalculationService commissionCalculationService;
    private final UserRepository userRepository;

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

        CommissionRule rule = CommissionRule.builder()
                .ruleCode(command.getRuleCode())
                .ruleName(command.getRuleName())
                .ruleType(command.getRuleType())
                .firmRetentionRate(command.getFirmRetentionRate())
                .originatorRate(command.getOriginatorRate())
                .taxRate(command.getTaxRate() != null ? command.getTaxRate() : 
                        java.math.BigDecimal.valueOf(0.0672))
                .managementFeeRate(command.getManagementFeeRate() != null ? 
                        command.getManagementFeeRate() : java.math.BigDecimal.valueOf(0.15))
                .rateTiers(command.getRateTiers())
                .effectiveDate(command.getEffectiveDate())
                .expiryDate(command.getExpiryDate())
                .isDefault(command.getIsDefault() != null ? command.getIsDefault() : false)
                .active(command.getActive() != null ? command.getActive() : true)
                .createdBy(SecurityUtils.getUserId())
                .build();

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
        if (command.getFirmRetentionRate() != null) {
            rule.setFirmRetentionRate(command.getFirmRetentionRate());
        }
        if (command.getOriginatorRate() != null) {
            rule.setOriginatorRate(command.getOriginatorRate());
        }
        if (command.getTaxRate() != null) {
            rule.setTaxRate(command.getTaxRate());
        }
        if (command.getManagementFeeRate() != null) {
            rule.setManagementFeeRate(command.getManagementFeeRate());
        }
        if (command.getRateTiers() != null) {
            rule.setRateTiers(command.getRateTiers());
        }
        if (command.getEffectiveDate() != null) {
            rule.setEffectiveDate(command.getEffectiveDate());
        }
        if (command.getExpiryDate() != null) {
            rule.setExpiryDate(command.getExpiryDate());
        }
        if (command.getIsDefault() != null) {
            rule.setIsDefault(command.getIsDefault());
        }
        if (command.getActive() != null) {
            rule.setActive(command.getActive());
        }

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
     * 计算提成（收款核销后自动触发）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<CommissionDTO> calculateCommission(Long paymentId) {
        List<Commission> commissions = commissionCalculationService.calculateCommission(paymentId);
        return commissions.stream().map(this::toCommissionDTO).collect(Collectors.toList());
    }

    // ========== 提成查询 ==========

    /**
     * 查询提成记录列表
     */
    public PageResult<CommissionDTO> listCommissions(CommissionQueryDTO query) {
        // 数据权限：普通律师只能看自己的提成（通过 commission_detail 表关联）
        Long currentUserId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(currentUserId);
        if (!roleCodes.contains("admin") && !roleCodes.contains("director") && !roleCodes.contains("partner")) {
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
        if (!roleCodes.contains("admin") && !roleCodes.contains("director") && !roleCodes.contains("partner")) {
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
        if (!roleCodes.contains("admin") && !roleCodes.contains("director") && !roleCodes.contains("partner")) {
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
        if (!roleCodes.contains("admin") && !roleCodes.contains("director") && !roleCodes.contains("partner") && !roleCodes.contains("finance")) {
            if (userId == null || !userId.equals(currentUserId)) {
                throw new BusinessException("无权查看提成报表");
            }
        }
        
        return commissionRepository.queryCommissionReportData(startDate, endDate, userId);
    }

    // ========== 权限检查 ==========

    /**
     * 检查是否有规则管理权限（仅admin/director）
     */
    private void checkRulePermission() {
        Long userId = SecurityUtils.getUserId();
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("admin") && !roleCodes.contains("director")) {
            throw new BusinessException("只有系统管理员和主任可以管理提成规则");
        }
    }

    /**
     * 检查是否有管理层权限（admin/director/partner）
     */
    private void checkManagementPermission() {
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("admin") && !roleCodes.contains("director") && !roleCodes.contains("partner")) {
            throw new BusinessException("只有管理层可以查看提成汇总");
        }
    }

    /**
     * 检查是否有主任权限（仅director）
     */
    private void checkDirectorPermission() {
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("admin") && !roleCodes.contains("director")) {
            throw new BusinessException("只有主任可以审批提成");
        }
    }

    /**
     * 检查是否有财务权限（finance角色）
     */
    private void checkFinancePermission() {
        Long userId = SecurityUtils.getUserId();
        List<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        if (!roleCodes.contains("admin") && !roleCodes.contains("finance")) {
            throw new BusinessException("只有财务人员可以确认提成发放");
        }
    }

    // ========== DTO转换 ==========

    private CommissionRuleDTO toDTO(CommissionRule rule) {
        CommissionRuleDTO dto = new CommissionRuleDTO();
        BeanUtils.copyProperties(rule, dto);
        return dto;
    }

    private CommissionDTO toCommissionDTO(Commission commission) {
        CommissionDTO dto = new CommissionDTO();
        BeanUtils.copyProperties(commission, dto);
        
        // 注意：Commission 表没有 userId 字段，提成是通过 commission_detail 表分配给用户的
        // 如果需要显示用户信息，应该从 commission_detail 表中查询
        
        return dto;
    }
}
