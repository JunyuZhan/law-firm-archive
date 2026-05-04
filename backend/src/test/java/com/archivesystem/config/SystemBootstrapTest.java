package com.archivesystem.config;

import com.archivesystem.entity.Role;
import com.archivesystem.entity.User;
import com.archivesystem.repository.RoleMapper;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.repository.UserRoleMapper;
import com.archivesystem.security.RuntimeSecretProvider;
import com.archivesystem.service.ConfigService;
import com.archivesystem.service.MinioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemBootstrapTest {

    @Mock
    private ConfigService configService;

    @Mock
    private RuntimeSecretProvider runtimeSecretProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @InjectMocks
    private SystemBootstrap systemBootstrap;

    @Test
    void run_shouldNotOverrideExistingProxyPrefix() throws Exception {
        when(configService.getValue(MinioService.CONFIG_KEY_PROXY_PREFIX)).thenReturn("/custom-storage");

        systemBootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(configService, never()).saveConfig(
                eq(MinioService.CONFIG_KEY_PROXY_PREFIX),
                any(),
                any(),
                any(),
                any(),
                any(),
                any());
    }

    @Test
    void run_shouldNotCreateDefaultUsersWhenSystemAlreadyInitialized() throws Exception {
        systemBootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(roleMapper).selectByRoleCode(User.TYPE_SYSTEM_ADMIN);
        verify(roleMapper).selectByRoleCode(User.TYPE_SECURITY_ADMIN);
        verify(roleMapper).selectByRoleCode(User.TYPE_AUDIT_ADMIN);
        verify(userMapper, never()).insert(any(User.class));
        verify(userRoleMapper, never()).insert(any());
    }

    @Test
    void run_shouldCreateMissingRolesWithoutCreatingUsers() throws Exception {
        when(roleMapper.selectByRoleCode(any())).thenReturn(null);

        systemBootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(roleMapper, org.mockito.Mockito.times(3)).insert(any(Role.class));
        verify(userMapper, never()).insert(any(User.class));
    }
}
