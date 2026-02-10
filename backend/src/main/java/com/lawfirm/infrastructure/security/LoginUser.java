package com.lawfirm.infrastructure.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 登录用户信息（Spring Security UserDetails实现）
 *
 * <p>支持Redis缓存序列化（UserDetails已继承Serializable）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginUser implements UserDetails {

  @Serial private static final long serialVersionUID = 1L;

  /** 用户ID. */
  private Long userId;

  /** 用户名. */
  private String username;

  /** 密码（序列化时忽略，防止泄露）. */
  @JsonIgnore
  private String password;

  /** 真实姓名. */
  private String realName;

  /** 部门ID. */
  private Long departmentId;

  /** 薪酬模式. */
  private String compensationType;

  /** 角色编码列表. */
  private Set<String> roles;

  /** 权限编码列表. */
  private Set<String> permissions;

  /** 数据范围：ALL, DEPT_AND_CHILD, DEPT, SELF. */
  private String dataScope;

  /** 账号是否启用. */
  @lombok.Builder.Default private boolean enabled = true;

  /**
   * 获取权限集合.
   *
   * @return 权限集合
   */
  @Override
  @JsonIgnore
  public Collection<? extends GrantedAuthority> getAuthorities() {
    // 将角色和权限转换为GrantedAuthority
    List<SimpleGrantedAuthority> authorities =
        permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

    // 角色以ROLE_前缀添加
    roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));

    return authorities;
  }

  /**
   * 获取密码.
   *
   * @return 密码
   */
  @Override
  public String getPassword() {
    return password;
  }

  /**
   * 获取用户名.
   *
   * @return 用户名
   */
  @Override
  public String getUsername() {
    return username;
  }

  /**
   * 账号是否未过期.
   *
   * @return true表示未过期
   */
  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 账号是否未锁定.
   *
   * @return true表示未锁定
   */
  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  /**
   * 凭证是否未过期.
   *
   * @return true表示未过期
   */
  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 账号是否启用.
   *
   * @return true表示启用
   */
  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
