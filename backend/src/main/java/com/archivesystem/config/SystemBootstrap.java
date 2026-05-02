package com.archivesystem.config;

import com.archivesystem.entity.Role;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.entity.User;
import com.archivesystem.entity.UserRole;
import com.archivesystem.repository.RoleMapper;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.repository.UserRoleMapper;
import com.archivesystem.security.RuntimeSecretProvider;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 系统首启引导：补齐关键配置、管理员账号与运行时密钥。
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class SystemBootstrap implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    private final ConfigService configService;
    private final RuntimeSecretProvider runtimeSecretProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public void run(ApplicationArguments args) {
        ensureRuntimeSecrets();
        ensureBusinessDefaults();
        ensureAdminAccounts();
    }

    private void ensureRuntimeSecrets() {
        runtimeSecretProvider.getJwtSecret();
        runtimeSecretProvider.getCryptoSecret();
        runtimeSecretProvider.getLegacyCryptoSecret();
    }

    private void ensureBusinessDefaults() {
        ensureConfigIfMissing(
                MinioService.CONFIG_KEY_PROXY_PREFIX,
                "/storage",
                SysConfig.GROUP_SYSTEM,
                "MinIO 代理前缀（用于将预签名地址转换为 Nginx 网关路径）",
                SysConfig.TYPE_STRING,
                true,
                27
        );
    }

    private void ensureAdminAccounts() {
        Long existingUsers = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, false));
        if (existingUsers != null && existingUsers > 0) {
            log.info("检测到系统已存在用户，跳过默认管理账号初始化");
            return;
        }

        ensureRole(User.TYPE_SYSTEM_ADMIN, "系统管理员", "系统配置、用户管理、系统维护");
        ensureRole(User.TYPE_SECURITY_ADMIN, "安全保密员", "权限管理、密级管理、安全策略");
        ensureRole(User.TYPE_AUDIT_ADMIN, "安全审计员", "日志审计、安全审查、合规检查");

        ensureUser("admin", "系统管理员", "admin@archive.com", "13800000001", "信息技术部", User.TYPE_SYSTEM_ADMIN);
        ensureUser("security", "安全保密员", "security@archive.com", "13800000002", "安全保密部", User.TYPE_SECURITY_ADMIN);
        ensureUser("auditor", "安全审计员", "auditor@archive.com", "13800000003", "审计部", User.TYPE_AUDIT_ADMIN);
    }

    private void ensureRole(String roleCode, String roleName, String description) {
        Role existing = roleMapper.selectByRoleCode(roleCode);
        if (existing != null) {
            return;
        }
        Role role = Role.builder()
                .roleCode(roleCode)
                .roleName(roleName)
                .description(description)
                .status(Role.STATUS_ACTIVE)
                .deleted(false)
                .build();
        roleMapper.insert(role);
        log.info("补齐默认角色: {}", roleCode);
    }

    private void ensureUser(
            String username,
            String realName,
            String email,
            String phone,
            String department,
            String userType) {
        User existing = userMapper.selectByUsername(username);
        if (existing == null) {
            existing = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                    .realName(realName)
                    .email(email)
                    .phone(phone)
                    .department(department)
                    .userType(userType)
                    .status(User.STATUS_ACTIVE)
                    .build();
            userMapper.insert(existing);
            log.warn("空库自动创建默认管理账号: username={}, initialPassword={}", username, DEFAULT_ADMIN_PASSWORD);
        }
        ensureUserRole(existing.getId(), userType);
    }

    private void ensureUserRole(Long userId, String roleCode) {
        if (userId == null) {
            return;
        }
        Role role = roleMapper.selectByRoleCode(roleCode);
        if (role == null || role.getId() == null) {
            return;
        }
        List<Role> existingRoles = roleMapper.selectByUserId(userId);
        boolean alreadyBound = existingRoles.stream()
                .anyMatch(item -> roleCode.equals(item.getRoleCode()));
        if (alreadyBound) {
            return;
        }
        userRoleMapper.insert(UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .build());
        log.info("补齐用户角色绑定: userId={}, roleCode={}", userId, roleCode);
    }

    private void ensureConfigIfMissing(
            String key,
            String value,
            String group,
            String description,
            String type,
            Boolean editable,
            Integer sortOrder) {
        String existingValue = configService.getValue(key);
        if (existingValue != null) {
            return;
        }
        configService.saveConfig(key, value, group, description, type, editable, sortOrder);
    }
}
