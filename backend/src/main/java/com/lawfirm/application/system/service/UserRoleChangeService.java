package com.lawfirm.application.system.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.matter.entity.Matter;
import com.lawfirm.domain.system.entity.Role;
import com.lawfirm.domain.system.entity.RoleChangeLog;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.RoleChangeLogRepository;
import com.lawfirm.domain.system.repository.RoleRepository;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.domain.workbench.repository.ApprovalRepository;
import com.lawfirm.domain.finance.entity.Contract;
import com.lawfirm.infrastructure.persistence.mapper.FinanceContractMapper;
import com.lawfirm.infrastructure.persistence.mapper.MatterMapper;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色变更服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleChangeService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final RoleChangeLogRepository roleChangeLogRepository;
    private final ApprovalRepository approvalRepository;
    private final MatterMapper matterMapper;
    private final FinanceContractMapper financeContractMapper;
    private final PermissionCacheService permissionCacheService;

    /**
     * 检查用户角色变更前的待处理事项
     */
    public RoleChangeCheckResult checkRoleChange(Long userId, List<Long> newRoleIds) {
        User user = userRepository.getByIdOrThrow(userId, "用户不存在");
        List<Long> oldRoleIds = userMapper.selectRoleIdsByUserId(userId);
        
        // 判断变更类型
        RoleChangeType changeType = determineChangeType(oldRoleIds, newRoleIds);
        
        // 检查待处理业务
        int pendingCount = checkPendingBusiness(userId);
        
        // 构建检查结果
        RoleChangeCheckResult result = new RoleChangeCheckResult();
        result.setUserId(userId);
        result.setUsername(user.getUsername());
        result.setChangeType(changeType.name());
        result.setPendingBusinessCount(pendingCount);
        result.setCanChange(pendingCount == 0 || changeType == RoleChangeType.UPGRADE);
        
        if (pendingCount > 0 && changeType != RoleChangeType.UPGRADE) {
            result.setWarningMessage(String.format("用户有%d项待处理业务，建议先处理后再变更角色", pendingCount));
        }
        
        return result;
    }

    /**
     * 变更用户角色
     */
    @Transactional
    public void changeUserRole(Long userId, List<Long> newRoleIds, String reason) {
        // 1. 获取用户当前角色
        User user = userRepository.getByIdOrThrow(userId, "用户不存在");
        List<Long> oldRoleIds = userMapper.selectRoleIdsByUserId(userId);
        List<String> oldRoleCodes = userRepository.findRoleCodesByUserId(userId);
        
        // 2. 检查角色变更类型
        RoleChangeType changeType = determineChangeType(oldRoleIds, newRoleIds);
        
        // 3. 检查待处理业务（权限缩小时）
        int pendingCount = 0;
        if (changeType == RoleChangeType.DOWNGRADE || changeType == RoleChangeType.TRANSFER) {
            pendingCount = checkPendingBusiness(userId);
            if (pendingCount > 0) {
                log.warn("用户{}有{}项待处理业务，建议先处理", userId, pendingCount);
                // 可以选择抛出异常或仅记录警告
                // throw new BusinessException("用户有" + pendingCount + "项待处理业务，请先处理后再变更角色");
            }
        }
        
        // 4. 更新用户角色
        userMapper.deleteUserRoles(userId);
        if (!newRoleIds.isEmpty()) {
            userMapper.insertUserRoles(userId, newRoleIds);
        }
        
        // 5. 获取新角色信息
        List<String> newRoleCodes = newRoleIds.stream()
                .map(roleId -> {
                    Role role = roleRepository.getById(roleId);
                    return role != null ? role.getRoleCode() : null;
                })
                .filter(code -> code != null)
                .collect(Collectors.toList());
        
        // 6. 记录变更历史
        RoleChangeLog changeLog = RoleChangeLog.builder()
                .userId(userId)
                .username(user.getUsername())
                .oldRoleIds(JSONUtil.toJsonStr(oldRoleIds))
                .oldRoleCodes(JSONUtil.toJsonStr(oldRoleCodes))
                .newRoleIds(JSONUtil.toJsonStr(newRoleIds))
                .newRoleCodes(JSONUtil.toJsonStr(newRoleCodes))
                .changeType(changeType.name())
                .changeReason(reason)
                .pendingBusinessCount(pendingCount)
                .changedBy(SecurityUtils.getUserId())
                .changedAt(LocalDateTime.now())
                .build();
        roleChangeLogRepository.save(changeLog);
        
        // 7. 清除权限缓存
        clearUserPermissionCache(userId);
        
        log.info("用户角色变更成功: userId={}, oldRoles={}, newRoles={}", 
                userId, oldRoleCodes, newRoleCodes);
    }

    /**
     * 检查待处理业务
     */
    private int checkPendingBusiness(Long userId) {
        int count = 0;
        
        // 1. 检查待审批的审批记录
        count += approvalRepository.countPendingByApproverId(userId);
        
        // 2. 检查进行中的项目（通过主办律师）
        // 查询作为主办律师的进行中项目（状态不是CLOSED）
        long leadMatterCount = matterMapper.selectCount(
                new LambdaQueryWrapper<Matter>()
                        .eq(Matter::getLeadLawyerId, userId)
                        .ne(Matter::getStatus, "CLOSED")
                        .eq(Matter::getDeleted, false)
        );
        count += (int) leadMatterCount;
        
        // 查询作为参与人的进行中项目（通过matter_participant表）
        // 注意：这里简化处理，只统计主办律师的项目
        // 如果需要统计参与人的项目，需要添加新的Mapper方法
        
        // 3. 检查待处理的合同（状态为PENDING的合同）
        long pendingContractCount = financeContractMapper.selectCount(
                new LambdaQueryWrapper<Contract>()
                        .and(w -> w
                                .eq(Contract::getSignerId, userId)
                                .or()
                                .eq(Contract::getCreatedBy, userId)
                        )
                        .eq(Contract::getStatus, "PENDING")
                        .eq(Contract::getDeleted, false)
        );
        count += (int) pendingContractCount;
        
        return count;
    }

    /**
     * 清除用户权限缓存（委托给PermissionCacheService）
     */
    public void clearUserPermissionCache(Long userId) {
        permissionCacheService.clearUserPermissionCache(userId);
    }

    /**
     * 批量清除权限缓存（委托给PermissionCacheService）
     */
    public void batchClearPermissionCache(List<Long> userIds) {
        permissionCacheService.batchClearPermissionCache(userIds);
    }

    /**
     * 判断角色变更类型
     */
    private RoleChangeType determineChangeType(List<Long> oldRoleIds, List<Long> newRoleIds) {
        // 简化实现：根据角色数量判断
        // 实际应该根据角色的权限范围和数据范围来判断
        if (newRoleIds.size() > oldRoleIds.size()) {
            return RoleChangeType.UPGRADE;
        } else if (newRoleIds.size() < oldRoleIds.size()) {
            return RoleChangeType.DOWNGRADE;
        } else {
            // 数量相同，可能是跨部门或跨角色
            // 简化处理：如果角色ID不同，认为是TRANSFER
            if (!oldRoleIds.equals(newRoleIds)) {
                return RoleChangeType.TRANSFER;
            }
            return RoleChangeType.UPGRADE; // 默认
        }
    }

    /**
     * 角色变更类型枚举
     */
    public enum RoleChangeType {
        UPGRADE,    // 权限扩大
        DOWNGRADE,  // 权限缩小
        TRANSFER    // 跨部门/跨角色
    }

    /**
     * 角色变更检查结果
     */
    @lombok.Data
    public static class RoleChangeCheckResult {
        private Long userId;
        private String username;
        private String changeType;
        private int pendingBusinessCount;
        private boolean canChange;
        private String warningMessage;
    }
}

