package com.archivesystem.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全工具类.
 * @author junyuzhan
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID.
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getId();
        }
        return null;
    }

    /**
     * 获取当前登录用户名.
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }

    /**
     * 获取当前登录用户真实姓名.
     */
    public static String getCurrentRealName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getRealName();
        }
        return null;
    }

    /**
     * 获取当前登录用户部门.
     */
    public static String getCurrentDepartment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails.getDepartment();
        }
        return null;
    }

    /**
     * 获取当前登录用户详情.
     */
    public static UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            return userDetails;
        }
        return null;
    }

    /**
     * 判断当前用户是否已认证.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof UserDetailsImpl;
    }

    /**
     * 判断当前用户是否是系统管理员.
     */
    public static boolean isSystemAdmin() {
        UserDetailsImpl user = getCurrentUser();
        return user != null && "SYSTEM_ADMIN".equals(user.getUserType());
    }

    /**
     * 获取当前用户类型.
     */
    public static String getCurrentUserType() {
        UserDetailsImpl user = getCurrentUser();
        return user != null ? UserRoleUtils.normalize(user.getUserType()) : null;
    }

    /**
     * 判断当前用户是否拥有指定角色之一.
     * @param roles 角色列表（不含 ROLE_ 前缀）
     */
    public static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        
        for (String role : roles) {
            String roleWithPrefix = "ROLE_" + role;
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(roleWithPrefix) || 
                                      auth.getAuthority().equals(role));
            if (hasRole) {
                return true;
            }
        }
        return false;
    }
}
