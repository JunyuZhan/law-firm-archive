package com.archivesystem.service;

import com.archivesystem.common.PageResult;
import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.User;
import com.archivesystem.entity.UserRole;
import com.archivesystem.repository.UserMapper;
import com.archivesystem.repository.UserRoleMapper;
import com.archivesystem.service.impl.UserServiceImpl;
import com.archivesystem.security.PasswordValidator;
import com.archivesystem.service.OperationLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRoleMapper userRoleMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidator passwordValidator;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRealName("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setStatus(User.STATUS_ACTIVE);

        // 设置PasswordValidator的默认stubbing
        PasswordValidator.ValidationResult validResult = new PasswordValidator.ValidationResult(true, null, 80);
        when(passwordValidator.validate(anyString())).thenReturn(validResult);
    }

    @Test
    void testCreateUser_Success() {
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        User result = userService.create(testUser);

        assertNotNull(result);
        assertEquals(User.STATUS_ACTIVE, result.getStatus());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void testCreateUser_UsernameExists() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        assertThrows(BusinessException.class, () -> userService.create(testUser));
    }

    @Test
    void testGetById_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        User result = userService.getById(1L);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertNull(result.getPassword());
    }

    @Test
    void testGetById_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userService.getById(999L));
    }

    @Test
    void testGetByUsername() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        User result = userService.getByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testDeleteUser() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userRoleMapper.deleteByUserId(1L)).thenReturn(1);
        when(userMapper.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> userService.delete(1L));

        verify(userRoleMapper, times(1)).deleteByUserId(1L);
        verify(userMapper, times(1)).deleteById(1L);
    }

    @Test
    void testResetPassword() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.resetPassword(1L, "newPassword123"));

        verify(passwordEncoder).encode("newPassword123");
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testResetPassword_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userService.resetPassword(999L, "newPassword123"));
    }

    @Test
    void testChangePassword_Success() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword", "password123")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.changePassword(1L, "oldPassword", "newPassword"));

        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongPassword", "password123")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.changePassword(1L, "wrongPassword", "newPassword"));
    }

    @Test
    void testUpdateStatus() {
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.updateStatus(1L, User.STATUS_DISABLED));

        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testAssignRoles() {
        List<Long> roleIds = Arrays.asList(1L, 2L);
        
        when(userRoleMapper.deleteByUserId(1L)).thenReturn(1);
        when(userRoleMapper.insert(any(UserRole.class))).thenReturn(1);

        assertDoesNotThrow(() -> userService.assignRoles(1L, roleIds));

        verify(userRoleMapper, times(1)).deleteByUserId(1L);
        verify(userRoleMapper, times(2)).insert(any(UserRole.class));
    }

    @Test
    void testGetUserRoleIds() {
        UserRole userRole1 = new UserRole();
        userRole1.setUserId(1L);
        userRole1.setRoleId(1L);
        
        UserRole userRole2 = new UserRole();
        userRole2.setUserId(1L);
        userRole2.setRoleId(2L);

        List<UserRole> userRoles = Arrays.asList(userRole1, userRole2);
        
        when(userRoleMapper.selectList(any())).thenReturn(userRoles);

        List<Long> result = userService.getUserRoleIds(1L);

        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
    }

    @Test
    void testUpdateUser_Success() {
        User updateData = new User();
        updateData.setUsername("testuser"); // 同一用户名
        updateData.setRealName("更新用户名");
        updateData.setEmail("update@example.com");
        updateData.setPhone("13900139000");
        updateData.setUserType("ADMIN");
        updateData.setDepartment("管理部");
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        User result = userService.update(1L, updateData);

        assertNotNull(result);
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userService.update(999L, testUser));
    }

    @Test
    void testUpdateUser_UsernameExistsForOther() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otherusername");
        
        User updateData = new User();
        updateData.setUsername("otherusername");
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectByUsername("otherusername")).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> userService.update(1L, updateData));
    }

    @Test
    void testUpdateUser_ChangeUsernameSameId() {
        User sameUser = new User();
        sameUser.setId(1L);
        sameUser.setUsername("newusername");
        
        User updateData = new User();
        updateData.setUsername("newusername");
        updateData.setRealName("测试");
        
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(userMapper.selectByUsername("newusername")).thenReturn(sameUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        User result = userService.update(1L, updateData);

        assertNotNull(result);
    }

    @Test
    void testDeleteUser_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        // 不应该抛出异常，直接返回
        assertDoesNotThrow(() -> userService.delete(999L));
        
        verify(userRoleMapper, never()).deleteByUserId(anyLong());
    }

    @Test
    void testUpdateStatus_NotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userService.updateStatus(999L, User.STATUS_DISABLED));
    }

    @Test
    void testChangePassword_UserNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userService.changePassword(999L, "old", "new"));
    }

    @Test
    void testAssignRoles_EmptyList() {
        when(userRoleMapper.deleteByUserId(1L)).thenReturn(1);

        assertDoesNotThrow(() -> userService.assignRoles(1L, Collections.emptyList()));

        verify(userRoleMapper).deleteByUserId(1L);
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void testAssignRoles_NullList() {
        when(userRoleMapper.deleteByUserId(1L)).thenReturn(1);

        assertDoesNotThrow(() -> userService.assignRoles(1L, null));

        verify(userRoleMapper).deleteByUserId(1L);
        verify(userRoleMapper, never()).insert(any(UserRole.class));
    }

    @Test
    void testGetByUsername_NotFound() {
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        User result = userService.getByUsername("nonexistent");

        assertNull(result);
    }
}
