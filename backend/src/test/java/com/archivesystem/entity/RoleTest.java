package com.archivesystem.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
/**
 * @author junyuzhan
 */

class RoleTest {

    @Test
    void testBuilder() {
        LocalDateTime now = LocalDateTime.now();

        Role role = Role.builder()
                .id(1L)
                .roleCode("ADMIN")
                .roleName("管理员")
                .description("系统管理员角色")
                .status(Role.STATUS_ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        assertEquals(1L, role.getId());
        assertEquals("ADMIN", role.getRoleCode());
        assertEquals("管理员", role.getRoleName());
        assertEquals("系统管理员角色", role.getDescription());
        assertEquals(Role.STATUS_ACTIVE, role.getStatus());
        assertEquals(now, role.getCreatedAt());
        assertEquals(now, role.getUpdatedAt());
        assertFalse(role.getDeleted());
    }

    @Test
    void testDefaultValues() {
        Role role = Role.builder().build();

        assertEquals(Role.STATUS_ACTIVE, role.getStatus());
        assertFalse(role.getDeleted());
    }

    @Test
    void testNoArgsConstructor() {
        Role role = new Role();

        assertNull(role.getId());
        assertNull(role.getRoleCode());
        assertNull(role.getRoleName());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Role role = new Role(1L, "USER", "普通用户", "普通用户角色",
                Role.STATUS_ACTIVE, now, now, false);

        assertEquals(1L, role.getId());
        assertEquals("USER", role.getRoleCode());
        assertEquals("普通用户", role.getRoleName());
    }

    @Test
    void testStatusConstants() {
        assertEquals("ACTIVE", Role.STATUS_ACTIVE);
        assertEquals("DISABLED", Role.STATUS_DISABLED);
    }

    @Test
    void testSettersAndGetters() {
        Role role = new Role();

        role.setId(2L);
        role.setRoleCode("ARCHIVIST");
        role.setRoleName("档案员");
        role.setDescription("档案管理员");
        role.setStatus(Role.STATUS_DISABLED);

        assertEquals(2L, role.getId());
        assertEquals("ARCHIVIST", role.getRoleCode());
        assertEquals("档案员", role.getRoleName());
        assertEquals("档案管理员", role.getDescription());
        assertEquals(Role.STATUS_DISABLED, role.getStatus());
    }

    @Test
    void testEqualsAndHashCode() {
        Role role1 = new Role();
        role1.setId(1L);
        role1.setRoleCode("ADMIN");

        Role role2 = new Role();
        role2.setId(1L);
        role2.setRoleCode("ADMIN");

        assertEquals(role1, role2);
        assertEquals(role1.hashCode(), role2.hashCode());
    }

    @Test
    void testToString() {
        Role role = Role.builder()
                .id(1L)
                .roleCode("TEST")
                .build();

        String str = role.toString();
        assertNotNull(str);
        assertTrue(str.contains("Role"));
    }
}
