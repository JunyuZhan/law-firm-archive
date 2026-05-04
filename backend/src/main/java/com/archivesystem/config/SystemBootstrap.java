package com.archivesystem.config;

import com.archivesystem.entity.Role;
import com.archivesystem.entity.SysConfig;
import com.archivesystem.entity.User;
import com.archivesystem.repository.RoleMapper;
import com.archivesystem.security.RuntimeSecretProvider;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 系统首启引导：补齐关键配置、默认角色与运行时密钥。
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class SystemBootstrap implements ApplicationRunner {

    private final ConfigService configService;
    private final RuntimeSecretProvider runtimeSecretProvider;
    private final RoleMapper roleMapper;

    @Override
    public void run(ApplicationArguments args) {
        ensureRuntimeSecrets();
        ensureBusinessDefaults();
        ensureSystemRoles();
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

    private void ensureSystemRoles() {
        ensureRole(User.TYPE_SYSTEM_ADMIN, "系统管理员", "系统配置、用户管理、系统维护");
        ensureRole(User.TYPE_SECURITY_ADMIN, "安全保密员", "权限管理、密级管理、安全策略");
        ensureRole(User.TYPE_AUDIT_ADMIN, "安全审计员", "日志审计、安全审查、合规检查");
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
