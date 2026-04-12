package com.archivesystem.service;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Role;
import com.archivesystem.entity.UserRole;
import com.archivesystem.repository.RoleMapper;
import com.archivesystem.repository.UserRoleMapper;
import com.archivesystem.service.impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
/**
 * @author junyuzhan
 */

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setRoleCode("ROLE_ADMIN");
        testRole.setRoleName("管理员");
        testRole.setDescription("系统管理员");
        testRole.setStatus(Role.STATUS_ACTIVE);
    }

    @Test
    void testCreateRole_Success() {
        when(roleMapper.selectByRoleCode("ROLE_ADMIN")).thenReturn(null);
        when(roleMapper.insert(any(Role.class))).thenReturn(1);

        Role result = roleService.create(testRole);

        assertNotNull(result);
        assertEquals(Role.STATUS_ACTIVE, result.getStatus());
        verify(roleMapper).insert(any(Role.class));
    }

    @Test
    void testCreateRole_RoleCodeExists() {
        when(roleMapper.selectByRoleCode("ROLE_ADMIN")).thenReturn(testRole);

        assertThrows(BusinessException.class, () -> roleService.create(testRole));
    }

    @Test
    void testGetById_Success() {
        when(roleMapper.selectById(1L)).thenReturn(testRole);

        Role result = roleService.getById(1L);

        assertNotNull(result);
        assertEquals("ROLE_ADMIN", result.getRoleCode());
    }

    @Test
    void testGetById_NotFound() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> roleService.getById(999L));
    }

    @Test
    void testList() {
        List<Role> roles = Arrays.asList(testRole);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(roles);

        List<Role> result = roleService.list();

        assertEquals(1, result.size());
        assertEquals("ROLE_ADMIN", result.get(0).getRoleCode());
    }

    @Test
    void testList_Empty() {
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<Role> result = roleService.list();

        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateRole_Success() {
        when(roleMapper.selectById(1L)).thenReturn(testRole);
        when(roleMapper.updateById(any(Role.class))).thenReturn(1);

        Role updateData = new Role();
        updateData.setRoleName("超级管理员");
        updateData.setDescription("更新后的描述");

        Role result = roleService.update(1L, updateData);

        assertNotNull(result);
        verify(roleMapper).updateById(any(Role.class));
    }

    @Test
    void testUpdateRole_NotFound() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> roleService.update(999L, testRole));
    }

    @Test
    void testUpdateRole_RoleCodeExists() {
        Role existingRole = new Role();
        existingRole.setId(1L);
        existingRole.setRoleCode("ROLE_ADMIN");
        
        Role duplicateRole = new Role();
        duplicateRole.setId(2L);
        duplicateRole.setRoleCode("ROLE_USER");

        when(roleMapper.selectById(1L)).thenReturn(existingRole);
        when(roleMapper.selectByRoleCode("ROLE_USER")).thenReturn(duplicateRole);

        Role updateData = new Role();
        updateData.setRoleCode("ROLE_USER");

        assertThrows(BusinessException.class, () -> roleService.update(1L, updateData));
    }

    @Test
    void testDeleteRole_Success() {
        when(roleMapper.selectById(1L)).thenReturn(testRole);
        when(userRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(roleMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> roleService.delete(1L));

        verify(roleMapper).deleteById(1L);
    }

    @Test
    void testDeleteRole_HasUsers() {
        when(roleMapper.selectById(1L)).thenReturn(testRole);
        when(userRoleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        assertThrows(BusinessException.class, () -> roleService.delete(1L));
    }

    @Test
    void testDeleteRole_NotFound() {
        when(roleMapper.selectById(999L)).thenReturn(null);

        assertDoesNotThrow(() -> roleService.delete(999L));
    }
}
