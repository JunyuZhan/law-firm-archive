package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testBuilder() {
        LocalDateTime lastLoginAt = LocalDateTime.of(2026, 1, 15, 10, 30);

        User user = User.builder()
                .username("admin")
                .password("encodedPassword")
                .realName("管理员")
                .email("admin@example.com")
                .phone("13800138000")
                .department("技术部")
                .userType(User.TYPE_SYSTEM_ADMIN)
                .status(User.STATUS_ACTIVE)
                .lastLoginAt(lastLoginAt)
                .build();

        assertEquals("admin", user.getUsername());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals("管理员", user.getRealName());
        assertEquals("admin@example.com", user.getEmail());
        assertEquals("13800138000", user.getPhone());
        assertEquals("技术部", user.getDepartment());
        assertEquals(User.TYPE_SYSTEM_ADMIN, user.getUserType());
        assertEquals(User.STATUS_ACTIVE, user.getStatus());
        assertEquals(lastLoginAt, user.getLastLoginAt());
    }

    @Test
    void testDefaultValues() {
        User user = User.builder().build();

        assertEquals(User.TYPE_USER, user.getUserType());
        assertEquals(User.STATUS_ACTIVE, user.getStatus());
    }

    @Test
    void testNoArgsConstructor() {
        User user = new User();

        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getRealName());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User("user", "pass", "用户", "email@test.com",
                "13900139000", "研发部", User.TYPE_ARCHIVIST, User.STATUS_ACTIVE, now, "192.168.1.1");

        assertEquals("user", user.getUsername());
        assertEquals("pass", user.getPassword());
        assertEquals("用户", user.getRealName());
        assertEquals("email@test.com", user.getEmail());
        assertEquals("13900139000", user.getPhone());
        assertEquals("研发部", user.getDepartment());
        assertEquals(User.TYPE_ARCHIVIST, user.getUserType());
        assertEquals(User.STATUS_ACTIVE, user.getStatus());
        assertEquals(now, user.getLastLoginAt());
        assertEquals("192.168.1.1", user.getLastLoginIp());
    }

    @Test
    void testUserTypeConstants() {
        assertEquals("SYSTEM_ADMIN", User.TYPE_SYSTEM_ADMIN);
        assertEquals("SECURITY_ADMIN", User.TYPE_SECURITY_ADMIN);
        assertEquals("AUDIT_ADMIN", User.TYPE_AUDIT_ADMIN);
        assertEquals("ARCHIVIST", User.TYPE_ARCHIVIST);
        assertEquals("USER", User.TYPE_USER);
    }

    @Test
    void testStatusConstants() {
        assertEquals("ACTIVE", User.STATUS_ACTIVE);
        assertEquals("DISABLED", User.STATUS_DISABLED);
    }

    @Test
    void testSettersAndGetters() {
        User user = new User();

        user.setUsername("testuser");
        user.setPassword("password123");
        user.setRealName("测试用户");
        user.setEmail("test@example.com");
        user.setPhone("13700137000");
        user.setDepartment("销售部");
        user.setUserType(User.TYPE_SECURITY_ADMIN);
        user.setStatus(User.STATUS_DISABLED);

        assertEquals("testuser", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals("测试用户", user.getRealName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("13700137000", user.getPhone());
        assertEquals("销售部", user.getDepartment());
        assertEquals(User.TYPE_SECURITY_ADMIN, user.getUserType());
        assertEquals(User.STATUS_DISABLED, user.getStatus());
    }

    @Test
    void testToString() {
        User user = User.builder()
                .username("test")
                .realName("测试")
                .build();

        String str = user.toString();
        assertNotNull(str);
        assertTrue(str.contains("User"));
    }

    @Test
    void testEqualsAndHashCode() {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("admin");

        User user2 = new User();
        user2.setId(1L);
        user2.setUsername("admin");

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}
