package com.lawfirm.common.constant;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色类型枚举
 * ✅ 修复问题615: 将角色常量硬编码改为枚举
 * ✅ 修复问题616: 统一角色判断逻辑
 */
public enum RoleType {
    
    /** 系统管理员 */
    ADMIN("ADMIN", "系统管理员", true, true, true, true),
    
    /** 合伙人/团队负责人 */
    TEAM_LEADER("TEAM_LEADER", "合伙人", true, true, true, true),
    
    /** 主任 */
    DIRECTOR("DIRECTOR", "主任", true, true, true, true),
    
    /** 财务 */
    FINANCE("FINANCE", "财务", true, true, false, false),
    
    /** 行政 */
    ADMIN_STAFF("ADMIN_STAFF", "行政", true, false, false, false),
    
    /** 律师 */
    LAWYER("LAWYER", "律师", false, false, false, false),
    
    /** 助理 */
    ASSISTANT("ASSISTANT", "助理", false, false, false, false);

    private final String code;
    private final String description;
    private final boolean canAccessAllContracts;
    private final boolean canModifyFinance;
    private final boolean canManageTemplates;
    private final boolean isSeniorManagement;

    /** 高级管理角色集合 */
    public static final Set<RoleType> SENIOR_MANAGEMENT_ROLES = 
            EnumSet.of(ADMIN, TEAM_LEADER, DIRECTOR);
    
    /** 可访问所有合同的角色集合 */
    public static final Set<RoleType> ALL_CONTRACT_ACCESS_ROLES = 
            EnumSet.of(ADMIN, TEAM_LEADER, DIRECTOR, FINANCE, ADMIN_STAFF);
    
    /** 可修改财务信息的角色集合 */
    public static final Set<RoleType> FINANCE_MODIFY_ROLES = 
            EnumSet.of(ADMIN, TEAM_LEADER, DIRECTOR, FINANCE);
    
    /** 可管理模板的角色集合 */
    public static final Set<RoleType> TEMPLATE_MANAGE_ROLES = 
            EnumSet.of(ADMIN, TEAM_LEADER, DIRECTOR);

    RoleType(String code, String description, boolean canAccessAllContracts, 
             boolean canModifyFinance, boolean canManageTemplates, boolean isSeniorManagement) {
        this.code = code;
        this.description = description;
        this.canAccessAllContracts = canAccessAllContracts;
        this.canModifyFinance = canModifyFinance;
        this.canManageTemplates = canManageTemplates;
        this.isSeniorManagement = isSeniorManagement;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean canAccessAllContracts() {
        return canAccessAllContracts;
    }

    public boolean canModifyFinance() {
        return canModifyFinance;
    }

    public boolean canManageTemplates() {
        return canManageTemplates;
    }

    public boolean isSeniorManagement() {
        return isSeniorManagement;
    }

    /**
     * 根据角色代码获取枚举
     */
    public static RoleType fromCode(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查角色集合中是否包含高级管理角色
     */
    public static boolean hasSeniorManagement(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(RoleType::fromCode)
                .filter(r -> r != null)
                .anyMatch(RoleType::isSeniorManagement);
    }

    /**
     * 检查角色集合中是否可以访问所有合同
     */
    public static boolean canAccessAllContracts(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(RoleType::fromCode)
                .filter(r -> r != null)
                .anyMatch(RoleType::canAccessAllContracts);
    }

    /**
     * 检查角色集合中是否可以修改财务信息
     */
    public static boolean canModifyFinance(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(RoleType::fromCode)
                .filter(r -> r != null)
                .anyMatch(RoleType::canModifyFinance);
    }

    /**
     * 检查角色集合中是否可以管理模板
     */
    public static boolean canManageTemplates(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(RoleType::fromCode)
                .filter(r -> r != null)
                .anyMatch(RoleType::canManageTemplates);
    }

    /**
     * 获取高级管理角色代码集合
     */
    public static Set<String> getSeniorManagementCodes() {
        return SENIOR_MANAGEMENT_ROLES.stream()
                .map(RoleType::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * 获取模板管理角色代码集合
     */
    public static Set<String> getTemplateManageCodes() {
        return TEMPLATE_MANAGE_ROLES.stream()
                .map(RoleType::getCode)
                .collect(Collectors.toSet());
    }
}

