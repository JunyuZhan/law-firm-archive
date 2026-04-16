package com.archivesystem.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    private UserDetailsImpl testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDetailsImpl(
                1L,
                "testuser",
                "password",
                "测试用户",
                "诉讼部",
                "ADMIN",
                "ACTIVE",
                Collections.emptyList()
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testGetCurrentUserId_Authenticated() {
        setAuthentication(testUser);

        Long userId = SecurityUtils.getCurrentUserId();

        assertEquals(1L, userId);
    }

    @Test
    void testGetCurrentUserId_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        Long userId = SecurityUtils.getCurrentUserId();

        assertNull(userId);
    }

    @Test
    void testGetCurrentUsername_Authenticated() {
        setAuthentication(testUser);

        String username = SecurityUtils.getCurrentUsername();

        assertEquals("testuser", username);
    }

    @Test
    void testGetCurrentUsername_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        String username = SecurityUtils.getCurrentUsername();

        assertNull(username);
    }

    @Test
    void testGetCurrentRealName_Authenticated() {
        setAuthentication(testUser);

        String realName = SecurityUtils.getCurrentRealName();

        assertEquals("测试用户", realName);
    }

    @Test
    void testGetCurrentRealName_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        String realName = SecurityUtils.getCurrentRealName();

        assertNull(realName);
    }

    @Test
    void testGetCurrentDepartment_Authenticated() {
        setAuthentication(testUser);

        String department = SecurityUtils.getCurrentDepartment();

        assertEquals("诉讼部", department);
    }

    @Test
    void testGetCurrentUser_Authenticated() {
        setAuthentication(testUser);

        UserDetailsImpl currentUser = SecurityUtils.getCurrentUser();

        assertNotNull(currentUser);
        assertEquals(1L, currentUser.getId());
        assertEquals("testuser", currentUser.getUsername());
    }

    @Test
    void testGetCurrentUser_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        UserDetailsImpl currentUser = SecurityUtils.getCurrentUser();

        assertNull(currentUser);
    }

    @Test
    void testIsAuthenticated_True() {
        setAuthentication(testUser);

        assertTrue(SecurityUtils.isAuthenticated());
    }

    @Test
    void testIsAuthenticated_False_NoAuth() {
        SecurityContextHolder.clearContext();

        assertFalse(SecurityUtils.isAuthenticated());
    }

    @Test
    void testIsAuthenticated_False_WrongPrincipalType() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "stringPrincipal", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertFalse(SecurityUtils.isAuthenticated());
    }

    @Test
    void testIsSystemAdmin_True() {
        UserDetailsImpl adminUser = new UserDetailsImpl(
                1L, "admin", "password", "管理员", "管理部", "SYSTEM_ADMIN", "ACTIVE", Collections.emptyList()
        );
        setAuthentication(adminUser);

        assertTrue(SecurityUtils.isSystemAdmin());
    }

    @Test
    void testIsSystemAdmin_False() {
        setAuthentication(testUser);

        assertFalse(SecurityUtils.isSystemAdmin());
    }

    @Test
    void testIsSystemAdmin_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        assertFalse(SecurityUtils.isSystemAdmin());
    }

    @Test
    void testGetCurrentUserType_Authenticated() {
        setAuthentication(testUser);

        String userType = SecurityUtils.getCurrentUserType();

        // UserDetails 中历史别名 ADMIN 经 UserRoleUtils 归一化为 SYSTEM_ADMIN
        assertEquals("SYSTEM_ADMIN", userType);
    }

    @Test
    void testGetCurrentUserType_NotAuthenticated() {
        SecurityContextHolder.clearContext();

        String userType = SecurityUtils.getCurrentUserType();

        assertNull(userType);
    }

    @Test
    void testGetCurrentUserId_WithNullAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(null);

        Long userId = SecurityUtils.getCurrentUserId();

        assertNull(userId);
    }

    private void setAuthentication(UserDetailsImpl userDetails) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
