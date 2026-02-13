package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRoleTest {

    @Test
    void testBuilder() {
        UserRole userRole = UserRole.builder()
                .id(1L)
                .userId(100L)
                .roleId(10L)
                .build();

        assertEquals(1L, userRole.getId());
        assertEquals(100L, userRole.getUserId());
        assertEquals(10L, userRole.getRoleId());
    }

    @Test
    void testNoArgsConstructor() {
        UserRole userRole = new UserRole();

        assertNull(userRole.getId());
        assertNull(userRole.getUserId());
        assertNull(userRole.getRoleId());
    }

    @Test
    void testAllArgsConstructor() {
        UserRole userRole = new UserRole(1L, 100L, 10L);

        assertEquals(1L, userRole.getId());
        assertEquals(100L, userRole.getUserId());
        assertEquals(10L, userRole.getRoleId());
    }

    @Test
    void testSettersAndGetters() {
        UserRole userRole = new UserRole();

        userRole.setId(2L);
        userRole.setUserId(200L);
        userRole.setRoleId(20L);

        assertEquals(2L, userRole.getId());
        assertEquals(200L, userRole.getUserId());
        assertEquals(20L, userRole.getRoleId());
    }

    @Test
    void testEqualsAndHashCode() {
        UserRole userRole1 = new UserRole();
        userRole1.setId(1L);
        userRole1.setUserId(100L);
        userRole1.setRoleId(10L);

        UserRole userRole2 = new UserRole();
        userRole2.setId(1L);
        userRole2.setUserId(100L);
        userRole2.setRoleId(10L);

        assertEquals(userRole1, userRole2);
        assertEquals(userRole1.hashCode(), userRole2.hashCode());
    }

    @Test
    void testToString() {
        UserRole userRole = UserRole.builder()
                .id(1L)
                .userId(100L)
                .roleId(10L)
                .build();

        String str = userRole.toString();
        assertNotNull(str);
        assertTrue(str.contains("UserRole"));
    }

    @Test
    void testMultipleRolesPerUser() {
        // 模拟一个用户有多个角色
        UserRole adminRole = UserRole.builder()
                .id(1L)
                .userId(100L)
                .roleId(1L) // Admin role
                .build();

        UserRole userRoleRole = UserRole.builder()
                .id(2L)
                .userId(100L)
                .roleId(2L) // User role
                .build();

        assertEquals(100L, adminRole.getUserId());
        assertEquals(100L, userRoleRole.getUserId());
        assertNotEquals(adminRole.getRoleId(), userRoleRole.getRoleId());
    }
}
