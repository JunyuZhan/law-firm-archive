package com.archivesystem.security;

import com.archivesystem.entity.User;
import com.archivesystem.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户详情服务实现.
 * @author junyuzhan
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            // 不在异常消息中包含用户名，防止用户枚举攻击
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return UserDetailsImpl.build(user);
    }
}
