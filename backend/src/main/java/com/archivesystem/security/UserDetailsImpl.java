package com.archivesystem.security;

import com.archivesystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security 用户详情实现.
 * @author junyuzhan
 */
@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String realName;
    private String department;
    private String userType;
    private String status;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 从User实体构建UserDetails.
     */
    public static UserDetailsImpl build(User user) {
        String rawType = user.getUserType();
        Set<GrantedAuthority> authorities = UserRoleUtils.resolveGrantedRoles(rawType).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));

        String canonicalType = UserRoleUtils.normalize(rawType == null ? "" : rawType.trim());
        if (canonicalType == null || canonicalType.isBlank()) {
            canonicalType = User.TYPE_USER;
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getDepartment(),
                canonicalType,
                user.getStatus(),
                authorities
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
