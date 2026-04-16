package com.archivesystem.security;

import com.archivesystem.entity.User;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 用户角色归一化工具.
 * @author junyuzhan
 */
public final class UserRoleUtils {

    private UserRoleUtils() {
    }

    /**
     * 将历史角色归一化为当前产品角色（trim + 大小写不敏感，避免库内空格导致无权限）。
     */
    public static String normalize(String userType) {
        if (userType == null) {
            return null;
        }
        String t = userType.trim();
        if (t.isEmpty()) {
            return User.TYPE_USER;
        }
        String u = t.toUpperCase(Locale.ROOT);
        if ("ADMIN".equals(u)) {
            return User.TYPE_SYSTEM_ADMIN;
        }
        if (User.TYPE_ARCHIVIST.toUpperCase(Locale.ROOT).equals(u)) {
            return User.TYPE_ARCHIVE_MANAGER;
        }
        if (User.TYPE_SECURITY_ADMIN.toUpperCase(Locale.ROOT).equals(u)
                || User.TYPE_AUDIT_ADMIN.toUpperCase(Locale.ROOT).equals(u)) {
            return User.TYPE_SYSTEM_ADMIN;
        }
        if (User.TYPE_ARCHIVE_MANAGER.toUpperCase(Locale.ROOT).equals(u)) {
            return User.TYPE_ARCHIVE_MANAGER;
        }
        if (User.TYPE_ARCHIVE_REVIEWER.toUpperCase(Locale.ROOT).equals(u)) {
            return User.TYPE_ARCHIVE_REVIEWER;
        }
        if (User.TYPE_SYSTEM_ADMIN.toUpperCase(Locale.ROOT).equals(u)) {
            return User.TYPE_SYSTEM_ADMIN;
        }
        if (User.TYPE_USER.toUpperCase(Locale.ROOT).equals(u)) {
            return User.TYPE_USER;
        }
        return t;
    }

    /**
     * 返回当前账号应具备的授权角色集合（含兼容授权）。
     */
    public static Set<String> resolveGrantedRoles(String userType) {
        Set<String> roles = new LinkedHashSet<>();
        if (userType == null || userType.isBlank()) {
            roles.add(User.TYPE_USER);
            return roles;
        }
        String trimmed = userType.trim();
        roles.add(trimmed);
        String normalized = normalize(trimmed);
        if (normalized != null && !normalized.isBlank() && !normalized.equals(trimmed)) {
            roles.add(normalized);
        }
        return roles;
    }
}
