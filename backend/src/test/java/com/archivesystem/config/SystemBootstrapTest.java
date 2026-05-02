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
        when(userMapper.selectCount(any())).thenReturn(1L);

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
        when(userMapper.selectCount(any())).thenReturn(2L);

        systemBootstrap.run(new DefaultApplicationArguments(new String[0]));

        verify(userMapper, never()).selectByUsername(any());
        verify(userMapper, never()).insert(any(User.class));
        verify(roleMapper, never()).insert(any(Role.class));
        verify(userRoleMapper, never()).insert(any());
    }
}
