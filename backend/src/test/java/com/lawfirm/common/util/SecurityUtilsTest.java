package com.lawfirm.common.util;

import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.infrastructure.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SecurityUtils 单元测试
 *
 * 测试安全工具类的用户信息获取功能
 */
@DisplayName("SecurityUtils 安全工具类测试")
class SecurityUtilsTest {

    private static final Long USER_ID = 1L;
    private static final String USERNAME = "testuser";
    private static final String REAL_NAME = "测试用户";
    private static final Long DEPT_ID = 100L;
    private static final String COMPENSATION_TYPE = "COMMISSION";

    @BeforeEach
    void setUp() {
        setupAuthenticatedUser();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== 辅助方法 ==========

    private LoginUser createLoginUser() {
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        roles.add("ADMIN");

        Set<String> permissions = new HashSet<>();
        permissions.add("matter:read");
        permissions.add("matter:write");

        LoginUser user = new LoginUser();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);
        user.setRealName(REAL_NAME);
        user.setDepartmentId(DEPT_ID);
        user.setCompensationType(COMPENSATION_TYPE);
        user.setRoles(roles);
        user.setPermissions(permissions);
        user.setDataScope("ALL");

        return user;
    }

    private void setupAuthenticatedUser() {
        LoginUser user = createLoginUser();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // ========== getLoginUser 测试 ==========

    @Test
    @DisplayName("应该正确获取登录用户")
    void getLoginUser_shouldReturnLoginUser() {
        LoginUser user = SecurityUtils.getLoginUser();
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(USER_ID);
        assertThat(user.getUsername()).isEqualTo(USERNAME);
    }

    @Test
    @DisplayName("未登录时获取用户应该抛出异常")
    void getLoginUser_shouldThrowExceptionWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> SecurityUtils.getLoginUser())
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("用户未登录");
    }

    // ========== getUserId 测试 ==========

    @Test
    @DisplayName("应该正确获取用户ID")
    void getUserId_shouldReturnUserId() {
        assertThat(SecurityUtils.getUserId()).isEqualTo(USER_ID);
    }

    // ========== getCurrentUserId 测试 ==========

    @Test
    @DisplayName("getCurrentUserId 应该返回用户ID")
    void getCurrentUserId_shouldReturnUserId() {
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(USER_ID);
    }

    // ========== getUsername 测试 ==========

    @Test
    @DisplayName("应该正确获取用户名")
    void getUsername_shouldReturnUsername() {
        assertThat(SecurityUtils.getUsername()).isEqualTo(USERNAME);
    }

    // ========== getRealName 测试 ==========

    @Test
    @DisplayName("应该正确获取真实姓名")
    void getRealName_shouldReturnRealName() {
        assertThat(SecurityUtils.getRealName()).isEqualTo(REAL_NAME);
    }

    // ========== getDepartmentId 测试 ==========

    @Test
    @DisplayName("应该正确获取部门ID")
    void getDepartmentId_shouldReturnDepartmentId() {
        assertThat(SecurityUtils.getDepartmentId()).isEqualTo(DEPT_ID);
    }

    // ========== getCompensationType 测试 ==========

    @Test
    @DisplayName("应该正确获取薪酬模式")
    void getCompensationType_shouldReturnCompensationType() {
        assertThat(SecurityUtils.getCompensationType()).isEqualTo(COMPENSATION_TYPE);
    }

    // ========== getRoles 测试 ==========

    @Test
    @DisplayName("应该正确获取用户角色")
    void getRoles_shouldReturnUserRoles() {
        Set<String> roles = SecurityUtils.getRoles();
        assertThat(roles).isNotNull();
        assertThat(roles).contains("USER", "ADMIN");
    }

    // ========== getPermissions 测试 ==========

    @Test
    @DisplayName("应该正确获取用户权限")
    void getPermissions_shouldReturnUserPermissions() {
        Set<String> permissions = SecurityUtils.getPermissions();
        assertThat(permissions).isNotNull();
        assertThat(permissions).contains("matter:read", "matter:write");
    }

    // ========== hasPermission 测试 ==========

    @Test
    @DisplayName("应该正确判断是否有指定权限")
    void hasPermission_shouldCheckPermission() {
        assertThat(SecurityUtils.hasPermission("matter:read")).isTrue();
        assertThat(SecurityUtils.hasPermission("matter:delete")).isFalse();
    }

    // ========== hasRole 测试 ==========

    @Test
    @DisplayName("应该正确判断是否有指定角色")
    void hasRole_shouldCheckRole() {
        assertThat(SecurityUtils.hasRole("ADMIN")).isTrue();
        assertThat(SecurityUtils.hasRole("SUPER_ADMIN")).isFalse();
    }

    // ========== hasAnyRole 测试 ==========

    @Test
    @DisplayName("应该正确判断是否有任一指定角色")
    void hasAnyRole_shouldCheckAnyRole() {
        assertThat(SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")).isTrue();
        assertThat(SecurityUtils.hasAnyRole("USER", "GUEST")).isTrue();
        assertThat(SecurityUtils.hasAnyRole("GUEST", "ANONYMOUS")).isFalse();
    }

    // ========== isAdmin 测试 ==========

    @Test
    @DisplayName("应该正确判断是否是管理员")
    void isAdmin_shouldCheckAdminRole() {
        assertThat(SecurityUtils.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("非管理员用户应该返回false")
    void isAdmin_shouldReturnFalseForNonAdmin() {
        Set<String> roles = new HashSet<>();
        roles.add("USER");

        Set<String> permissions = new HashSet<>();
        permissions.add("matter:read");

        LoginUser user = new LoginUser();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);
        user.setRealName(REAL_NAME);
        user.setDepartmentId(DEPT_ID);
        user.setCompensationType(COMPENSATION_TYPE);
        user.setRoles(roles);
        user.setPermissions(permissions);
        user.setDataScope("SELF");

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.isAdmin()).isFalse();
    }

    // ========== isCommissionEligible 测试 ==========

    @Test
    @DisplayName("COMMISSION 薪酬模式应该有提成资格")
    void isCommissionEligible_shouldReturnTrueForCommission() {
        assertThat(SecurityUtils.isCommissionEligible()).isTrue();
    }

    // ========== getDataScope 测试 ==========

    @Test
    @DisplayName("应该正确获取数据范围")
    void getDataScope_shouldReturnDataScope() {
        assertThat(SecurityUtils.getDataScope()).isEqualTo("ALL");
    }

    @Test
    @DisplayName("未设置数据范围时应该返回SELF")
    void getDataScope_shouldReturnSelfWhenNull() {
        // 先清除现有认证
        SecurityContextHolder.clearContext();

        Set<String> roles = new HashSet<>();
        roles.add("USER");

        Set<String> permissions = new HashSet<>();
        permissions.add("matter:read");

        LoginUser user = new LoginUser();
        user.setUserId(USER_ID);
        user.setUsername(USERNAME);
        user.setRealName(REAL_NAME);
        user.setDepartmentId(DEPT_ID);
        user.setCompensationType(COMPENSATION_TYPE);
        user.setRoles(roles);
        user.setPermissions(permissions);
        user.setDataScope(null);  // explicitly set to null

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(SecurityUtils.getDataScope()).isEqualTo("SELF");
    }

    // ========== hasAllDataScope 测试 ==========

    @Test
    @DisplayName("应该正确判断是否有全所数据权限")
    void hasAllDataScope_shouldCheckAllScope() {
        assertThat(SecurityUtils.hasAllDataScope()).isTrue();
    }

    // ========== hasDeptAndChildDataScope 测试 ==========

    @Test
    @DisplayName("应该正确判断是否有部门及下级数据权限")
    void hasDeptAndChildDataScope_shouldCheckDeptAndChildScope() {
        assertThat(SecurityUtils.hasDeptAndChildDataScope()).isTrue();
    }

    // ========== hasDeptDataScope 测试 ==========

    @Test
    @DisplayName("应该正确判断是否有部门数据权限")
    void hasDeptDataScope_shouldCheckDeptScope() {
        assertThat(SecurityUtils.hasDeptDataScope()).isTrue();
    }

    // ========== getUserIdOrDefault 测试 ==========

    @Test
    @DisplayName("getUserIdOrDefault: 已登录应该返回用户ID")
    void getUserIdOrDefault_shouldReturnUserIdWhenAuthenticated() {
        assertThat(SecurityUtils.getUserIdOrDefault(null)).isEqualTo(USER_ID);
        assertThat(SecurityUtils.getUserIdOrDefault(999L)).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("getUserIdOrDefault: 未登录应该返回默认值")
    void getUserIdOrDefault_shouldReturnDefaultWhenNotAuthenticated() {
        SecurityContextHolder.clearContext();

        Long defaultUserId = 999L;
        assertThat(SecurityUtils.getUserIdOrDefault(defaultUserId)).isEqualTo(defaultUserId);
    }

    @Test
    @DisplayName("getUserIdOrDefault: 未登录且无默认值应该返回null")
    void getUserIdOrDefault_shouldReturnNullWhenNotAuthenticatedAndNoDefault() {
        SecurityContextHolder.clearContext();
        assertThat(SecurityUtils.getUserIdOrDefault(null)).isNull();
    }
}
