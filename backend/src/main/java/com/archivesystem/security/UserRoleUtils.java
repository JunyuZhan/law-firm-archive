package com.archivesystem.security;

import com.archivesystem.entity.User;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户角色归一化工具.
 * @author junyuzhan
 */
public final class UserRoleUtils {

    private UserRoleUtils() {
    }

    /**
     * 将历史角色归一化为当前产品角色。
     */
    public static String normalize(String userType) {
        if (User.TYPE_ARCHIVIST.equals(userType)) {
            return User.TYPE_ARCHIVE_MANAGER;
        }
        if (User.TYPE_SECURITY_ADMIN.equals(userType) || User.TYPE_AUDIT_ADMIN.equals(userType)) {
            return User.TYPE_SYSTEM_ADMIN;
        }
        return userType;
    }

    /**
     * 返回当前账号应具备的授权角色集合（含兼容授权）。
     */
    public static Set<String> resolveGrantedRoles(String userType) {
        Set<String> roles = new LinkedHashSet<>();
        if (userType == null || userType.isBlank()) {
            return roles;
        }

        roles.add(userType);
        String normalized = normalize(userType);
        if (normalized != null && !normalized.isBlank()) {
            roles.add(normalized);
        }
        return roles;
    }
}
