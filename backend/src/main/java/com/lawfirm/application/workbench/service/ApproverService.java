package com.lawfirm.application.workbench.service;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审批人查找服务
 * 根据业务类型和规则查找合适的审批人
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Value("${law-firm.approval.contract-amount-threshold:100000}")
    private java.math.BigDecimal contractAmountThreshold;

    /**
     * 查找合同审批人
     * 规则：根据合同金额和配置的审批角色
     */
    public Long findContractApprover(java.math.BigDecimal contractAmount) {
        // 如果合同金额超过阈值，需要合伙人审批
        if (contractAmount != null && contractAmount.compareTo(contractAmountThreshold) >= 0) {
            return findApproverByRole("PARTNER");
        }
        
        // 否则由主任或合伙人审批
        Long director = findApproverByRole("DIRECTOR");
        if (director != null) {
            return director;
        }
        
        return findApproverByRole("PARTNER");
    }

    /**
     * 查找用印申请审批人
     * 规则：由行政主管或合伙人审批
     */
    public Long findSealApplicationApprover() {
        Long admin = findApproverByRole("ADMIN");
        if (admin != null) {
            return admin;
        }
        
        return findApproverByRole("PARTNER");
    }

    /**
     * 查找利冲检查审批人
     * 规则：由合伙人或主任审批
     */
    public Long findConflictCheckApprover() {
        Long partner = findApproverByRole("PARTNER");
        if (partner != null) {
            return partner;
        }
        
        return findApproverByRole("DIRECTOR");
    }

    /**
     * 根据角色查找审批人
     * 返回第一个具有该角色的用户ID
     */
    private Long findApproverByRole(String roleCode) {
        List<Long> userIds = userMapper.selectUserIdsByRoleCode(roleCode);
        if (userIds != null && !userIds.isEmpty()) {
            return userIds.get(0);
        }
        
        log.warn("未找到角色为 {} 的审批人", roleCode);
        return null;
    }

    /**
     * 查找默认审批人（管理员）
     */
    public Long findDefaultApprover() {
        Long admin = findApproverByRole("ADMIN");
        if (admin != null) {
            return admin;
        }
        
        // 如果找不到管理员，返回第一个用户（通常是超级管理员）
        List<User> users = userRepository.list();
        if (users != null && !users.isEmpty()) {
            return users.get(0).getId();
        }
        
        throw new BusinessException("系统中没有可用的审批人");
    }
}

