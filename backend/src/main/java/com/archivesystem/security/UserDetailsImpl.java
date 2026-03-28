package com.archivesystem.security;

import com.archivesystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security 用户详情实现.
 */
@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String userType;
    private String status;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 从User实体构建UserDetails.
     */
    public static UserDetailsImpl build(User user) {
        // 根据用户类型生成权限
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getUserType());
        
        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getUserType(),
                user.getStatus(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return User.STATUS_ACTIVE.equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return User.STATUS_ACTIVE.equals(status);
    }
}
