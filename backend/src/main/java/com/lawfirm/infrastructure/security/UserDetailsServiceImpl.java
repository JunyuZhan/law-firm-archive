package com.lawfirm.infrastructure.security;

import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

/**
 * 用户详情服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 2. 检查用户状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 3. 查询角色和权限
        List<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        List<String> permissions = userRepository.findPermissionsByUserId(user.getId());
        
        // 4. 获取最高数据范围权限
        String dataScope = userRepository.findHighestDataScopeByUserId(user.getId());
        if (dataScope == null) {
            dataScope = "SELF";
        }

        // 5. 构建LoginUser
        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .departmentId(user.getDepartmentId())
                .compensationType(user.getCompensationType())
                .roles(new HashSet<>(roles))
                .permissions(new HashSet<>(permissions))
                .dataScope(dataScope)
                .enabled(true)
                .build();
    }
}

