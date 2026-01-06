package com.lawfirm.common.util;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.infrastructure.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

/**
 * 安全工具类 - 获取当前登录用户信息
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("401", "用户未登录");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }

        throw new BusinessException("401", "用户未登录");
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /**
     * 获取当前用户ID（别名方法）
     */
    public static Long getCurrentUserId() {
        return getUserId();
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 获取当前用户真实姓名
     */
    public static String getRealName() {
        return getLoginUser().getRealName();
    }

    /**
     * 获取当前用户部门ID
     */
    public static Long getDepartmentId() {
        return getLoginUser().getDepartmentId();
    }

    /**
     * 获取当前用户薪酬模式
     */
    public static String getCompensationType() {
        return getLoginUser().getCompensationType();
    }

    /**
     * 获取当前用户角色
     */
    public static Set<String> getRoles() {
        return getLoginUser().getRoles();
    }

    /**
     * 获取当前用户权限
     */
    public static Set<String> getPermissions() {
        return getLoginUser().getPermissions();
    }

    /**
     * 判断是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    /**
     * 判断是否有指定角色
     */
    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    /**
     * 判断是否是管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 判断当前用户是否有提成资格
     */
    public static boolean isCommissionEligible() {
        String type = getCompensationType();
        return "COMMISSION".equals(type) || "HYBRID".equals(type);
    }

    /**
     * 获取当前用户数据范围
     * @return ALL, DEPT_AND_CHILD, DEPT, SELF
     */
    public static String getDataScope() {
        String dataScope = getLoginUser().getDataScope();
        return dataScope != null ? dataScope : "SELF";
    }

    /**
     * 判断是否有全所数据权限
     */
    public static boolean hasAllDataScope() {
        return "ALL".equals(getDataScope());
    }

    /**
     * 判断是否有部门及下级数据权限
     */
    public static boolean hasDeptAndChildDataScope() {
        String scope = getDataScope();
        return "ALL".equals(scope) || "DEPT_AND_CHILD".equals(scope);
    }

    /**
     * 判断是否有部门数据权限
     */
    public static boolean hasDeptDataScope() {
        String scope = getDataScope();
        return "ALL".equals(scope) || "DEPT_AND_CHILD".equals(scope) || "DEPT".equals(scope);
    }
}

