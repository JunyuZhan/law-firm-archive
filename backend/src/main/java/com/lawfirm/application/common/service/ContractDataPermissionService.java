package com.lawfirm.application.common.service;

import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.finance.repository.ContractParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 合同数据权限服务
 * ✅ 修复问题603-604: 添加ThreadLocal缓存减少数据库查询
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractDataPermissionService {

    private final ContractParticipantRepository participantRepository;

    // 角色常量
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TEAM_LEADER = "TEAM_LEADER";
    private static final String ROLE_DIRECTOR = "DIRECTOR";
    private static final String ROLE_FINANCE = "FINANCE";
    private static final String ROLE_ADMIN_STAFF = "ADMIN_STAFF";
    private static final String ROLE_LAWYER = "LAWYER";

    /**
     * ✅ ThreadLocal缓存：在同一请求中缓存可访问的合同ID
     * 避免同一请求中多次查询数据库
     */
    private static final ThreadLocal<Set<Long>> ACCESSIBLE_CONTRACT_IDS_CACHE = new ThreadLocal<>();
    private static final ThreadLocal<Long> CACHED_USER_ID = new ThreadLocal<>();

    /**
     * 检查当前用户是否可以访问所有合同
     * 
     * Requirements: 8.1
     */
    public boolean canAccessAllContracts() {
        Set<String> roles = SecurityUtils.getRoles();
        
        // 管理员、合伙人、主任可以访问所有合同
        if (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_TEAM_LEADER) || roles.contains(ROLE_DIRECTOR)) {
            return true;
        }
        
        // 财务和行政可以访问所有合同（但字段受限）
        if (roles.contains(ROLE_FINANCE) || roles.contains(ROLE_ADMIN_STAFF)) {
            return true;
        }
        
        return false;
    }

    /**
     * 检查当前用户是否是财务角色
     * 
     * Requirements: 8.2
     */
    public boolean isFinanceRole() {
        return SecurityUtils.getRoles().contains(ROLE_FINANCE);
    }

    /**
     * 检查当前用户是否是行政角色
     * 
     * Requirements: 8.3
     */
    public boolean isAdminStaffRole() {
        return SecurityUtils.getRoles().contains(ROLE_ADMIN_STAFF);
    }

    /**
     * 检查当前用户是否是律师角色
     */
    public boolean isLawyerRole() {
        return SecurityUtils.getRoles().contains(ROLE_LAWYER);
    }

    /**
     * 检查当前用户是否是高级管理角色（合伙人/主任/管理员）
     */
    public boolean isSeniorManagement() {
        Set<String> roles = SecurityUtils.getRoles();
        return roles.contains(ROLE_ADMIN) || roles.contains(ROLE_TEAM_LEADER) || roles.contains(ROLE_DIRECTOR);
    }

    /**
     * 获取当前用户可访问的合同ID列表
     * 如果用户可以访问所有合同，返回null表示不需要过滤
     * ✅ 修复问题603: 使用ThreadLocal缓存减少重复查询
     * 
     * Requirements: 8.1
     */
    public List<Long> getAccessibleContractIds() {
        if (canAccessAllContracts()) {
            return null; // null表示可以访问所有合同
        }
        
        // 律师只能访问自己参与的合同
        Long userId = SecurityUtils.getCurrentUserId();
        List<Long> contractIds = participantRepository.findContractIdsByUserId(userId);
        
        // ✅ 缓存到ThreadLocal供canAccessContract使用
        if (contractIds != null && !contractIds.isEmpty()) {
            ACCESSIBLE_CONTRACT_IDS_CACHE.set(new HashSet<>(contractIds));
            CACHED_USER_ID.set(userId);
        }
        
        return contractIds;
    }

    /**
     * 检查当前用户是否可以访问指定合同
     * ✅ 修复问题604: 使用ThreadLocal缓存减少数据库查询
     * 
     * Requirements: 8.1
     */
    public boolean canAccessContract(Long contractId) {
        if (canAccessAllContracts()) {
            return true;
        }
        
        Long userId = SecurityUtils.getCurrentUserId();
        
        // ✅ 优先使用缓存（同一请求中）
        Set<Long> cachedIds = ACCESSIBLE_CONTRACT_IDS_CACHE.get();
        Long cachedUserId = CACHED_USER_ID.get();
        
        if (cachedIds != null && userId.equals(cachedUserId)) {
            return cachedIds.contains(contractId);
        }
        
        // 缓存未命中，查询数据库
        boolean hasAccess = participantRepository.existsByContractIdAndUserId(contractId, userId);
        
        // 如果有权限，将合同ID加入缓存
        if (hasAccess) {
            if (cachedIds == null) {
                cachedIds = new HashSet<>();
                CACHED_USER_ID.set(userId);
            }
            cachedIds.add(contractId);
            ACCESSIBLE_CONTRACT_IDS_CACHE.set(cachedIds);
        }
        
        return hasAccess;
    }
    
    /**
     * 清除ThreadLocal缓存（在请求结束时调用）
     * 建议在Filter或Interceptor中调用
     */
    public static void clearCache() {
        ACCESSIBLE_CONTRACT_IDS_CACHE.remove();
        CACHED_USER_ID.remove();
    }

    /**
     * 检查当前用户是否可以修改合同财务信息
     * 
     * Requirements: 8.2
     */
    public boolean canModifyFinanceInfo(Long contractId) {
        Set<String> roles = SecurityUtils.getRoles();
        
        // 管理员、合伙人、主任、财务可以修改财务信息
        if (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_TEAM_LEADER) || 
            roles.contains(ROLE_DIRECTOR) || roles.contains(ROLE_FINANCE)) {
            return true;
        }
        
        return false;
    }

    /**
     * 检查当前用户是否只能只读访问
     * 
     * Requirements: 8.3
     */
    public boolean isReadOnlyAccess() {
        Set<String> roles = SecurityUtils.getRoles();
        
        // 行政只能只读访问
        if (roles.contains(ROLE_ADMIN_STAFF) && !roles.contains(ROLE_ADMIN) && 
            !roles.contains(ROLE_TEAM_LEADER) && !roles.contains(ROLE_DIRECTOR)) {
            return true;
        }
        
        return false;
    }

    /**
     * 获取数据访问级别
     * 
     * @return ALL-全部, FINANCE-财务相关, BASIC-基本信息, SELF-仅自己参与的
     */
    public String getDataAccessLevel() {
        Set<String> roles = SecurityUtils.getRoles();
        
        if (roles.contains(ROLE_ADMIN) || roles.contains(ROLE_TEAM_LEADER) || roles.contains(ROLE_DIRECTOR)) {
            return "ALL";
        }
        
        if (roles.contains(ROLE_FINANCE)) {
            return "FINANCE";
        }
        
        if (roles.contains(ROLE_ADMIN_STAFF)) {
            return "BASIC";
        }
        
        return "SELF";
    }
}
