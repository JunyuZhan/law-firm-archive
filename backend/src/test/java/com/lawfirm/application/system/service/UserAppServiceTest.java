package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.CreateUserCommand;
import com.lawfirm.application.system.command.UpdateUserCommand;
import com.lawfirm.application.system.dto.UserDTO;
import com.lawfirm.application.system.dto.UserQueryDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.domain.system.repository.UserRepository;
import com.lawfirm.infrastructure.external.excel.ExcelImportExportService;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

/**
 * UserAppService 单元测试
 *
 * <p>测试用户应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserAppService 用户服务测试")
class UserAppServiceTest {

  private static final Long TEST_USER_ID = 1L;
  private static final Long TEST_DEPARTMENT_ID = 10L;

  @Mock private UserRepository userRepository;

  @Mock private UserMapper userMapper;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private ExcelImportExportService excelImportExportService;

  @Mock private UserRoleChangeService userRoleChangeService;

  @InjectMocks private UserAppService userAppService;

  @Nested
  @DisplayName("分页查询用户测试")
  class ListUsersTests {

    @Test
    @DisplayName("应该分页查询用户列表")
    void listUsers_shouldReturnPagedResult() {
      // Given
      UserQueryDTO query = new UserQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      User user = createTestUser(1L, "testuser", "测试用户");
      Page<User> page = new Page<>(1, 10);
      page.setRecords(List.of(user));
      page.setTotal(1);

      when(userMapper.selectUserPage(
              ArgumentMatchers.<Page<User>>any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(page);
      when(userMapper.selectRoleIdsByUserIds(anyList())).thenReturn(new ArrayList<>());

      // When
      PageResult<UserDTO> result = userAppService.listUsers(query);

      // Then
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getUsername()).isEqualTo("testuser");
      assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("空结果时应返回空分页")
    void listUsers_shouldReturnEmptyWhenNoResults() {
      // Given
      UserQueryDTO query = new UserQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      Page<User> page = new Page<>(1, 10);
      page.setRecords(new ArrayList<>());
      page.setTotal(0);

      when(userMapper.selectUserPage(
              ArgumentMatchers.<Page<User>>any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(page);

      // When
      PageResult<UserDTO> result = userAppService.listUsers(query);

      // Then
      assertThat(result.getRecords()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(0);
    }

    @Test
    @DisplayName("应该加载用户角色信息")
    void listUsers_shouldLoadUserRoles() {
      // Given
      UserQueryDTO query = new UserQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10);

      User user = createTestUser(1L, "testuser", "测试用户");
      Page<User> page = new Page<>(1, 10);
      page.setRecords(List.of(user));
      page.setTotal(1);

      List<UserMapper.UserRoleMapping> roleMappings = new ArrayList<>();
      UserMapper.UserRoleMapping mapping = new UserMapper.UserRoleMapping();
      mapping.setUserId(1L);
      mapping.setRoleId(100L);
      roleMappings.add(mapping);

      when(userMapper.selectUserPage(
              ArgumentMatchers.<Page<User>>any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(page);
      when(userMapper.selectRoleIdsByUserIds(anyList())).thenReturn(roleMappings);

      // When
      PageResult<UserDTO> result = userAppService.listUsers(query);

      // Then
      assertThat(result.getRecords().get(0).getRoleIds()).containsExactly(100L);
    }
  }

  @Nested
  @DisplayName("创建用户测试")
  class CreateUserTests {

    @Test
    @DisplayName("应该成功创建用户")
    void createUser_shouldReturnUserDTO() {
      // Given
      CreateUserCommand command =
          CreateUserCommand.builder()
              .username("newuser")
              .password("Valid@Pass123")
              .realName("新用户")
              .email("newuser@test.com")
              .phone("13800138000")
              .departmentId(TEST_DEPARTMENT_ID)
              .position("律师")
              .roleIds(List.of(1L, 2L))
              .build();

      when(userRepository.existsByUsername("newuser")).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
      // 使用Answer来模拟save时设置ID
      when(userRepository.save(any(User.class)))
          .thenAnswer(
              invocation -> {
                User user = invocation.getArgument(0);
                user.setId(TEST_USER_ID);
                return true;
              });
      doNothing().when(userMapper).insertUserRoles(eq(TEST_USER_ID), anyList());

      // When
      UserDTO result = userAppService.createUser(command);

      // Then
      assertThat(result.getUsername()).isEqualTo("newuser");
      assertThat(result.getRealName()).isEqualTo("新用户");
      verify(userRepository).save(any(User.class));
      verify(userMapper).insertUserRoles(eq(TEST_USER_ID), eq(List.of(1L, 2L)));
    }

    @Test
    @DisplayName("用户名已存在时应抛出异常")
    void createUser_shouldThrowException_whenUsernameExists() {
      // Given
      CreateUserCommand command =
          CreateUserCommand.builder()
              .username("existinguser")
              .password("Valid@Pass123")
              .realName("已存在用户")
              .build();

      when(userRepository.existsByUsername("existinguser")).thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> userAppService.createUser(command))
          .isInstanceOf(BusinessException.class)
          .hasMessage("用户名已存在");
    }

    @Test
    @DisplayName("应该使用默认值")
    void createUser_shouldUseDefaults() {
      // Given
      CreateUserCommand command =
          CreateUserCommand.builder()
              .username("newuser")
              .password("Valid@Pass123")
              .realName("新用户")
              .build();

      when(userRepository.existsByUsername("newuser")).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
      when(userRepository.save(any(User.class))).thenReturn(true);

      // When
      UserDTO result = userAppService.createUser(command);

      // Then
      assertThat(result.getCompensationType()).isEqualTo("COMMISSION");
      assertThat(result.getCanBeOriginator()).isTrue();
      assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }
  }

  @Nested
  @DisplayName("更新用户测试")
  class UpdateUserTests {

    @Test
    @DisplayName("应该成功更新用户")
    void updateUser_shouldReturnUserDTO() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        User existingUser = createTestUser(1L, "testuser", "测试用户");
        existingUser.setDepartmentId(TEST_DEPARTMENT_ID);

        UpdateUserCommand command = new UpdateUserCommand();
        command.setId(1L);
        command.setRealName("更新后的用户名");
        command.setEmail("updated@test.com");
        command.setRoleIds(List.of(1L));

        when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(existingUser);
        when(userMapper.selectRoleIdsByUserId(1L)).thenReturn(new ArrayList<>());
        when(userRepository.updateById(any(User.class))).thenReturn(true);

        // When
        UserDTO result = userAppService.updateUser(command);

        // Then
        assertThat(result.getRealName()).isEqualTo("更新后的用户名");
        verify(userRepository).updateById(any(User.class));
      }
    }

    @Test
    @DisplayName("用户不存在时应抛出异常")
    void updateUser_shouldThrowException_whenUserNotFound() {
      // Given
      UpdateUserCommand command = new UpdateUserCommand();
      command.setId(999L);

      when(userRepository.getByIdOrThrow(999L, "用户不存在")).thenThrow(new BusinessException("用户不存在"));

      // When & Then
      assertThatThrownBy(() -> userAppService.updateUser(command))
          .isInstanceOf(BusinessException.class)
          .hasMessage("用户不存在");
    }

    @Test
    @DisplayName("角色变更时应调用角色变更服务")
    void updateUser_shouldCallRoleChangeService_whenRolesChanged() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        User existingUser = createTestUser(1L, "testuser", "测试用户");

        UpdateUserCommand command = new UpdateUserCommand();
        command.setId(1L);
        command.setRoleIds(List.of(2L, 3L)); // 与原角色不同

        when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(existingUser);
        when(userMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(1L));
        when(userRepository.updateById(any(User.class))).thenReturn(true);
        doNothing().when(userRoleChangeService).changeUserRole(anyLong(), anyList(), anyString());

        // When
        userAppService.updateUser(command);

        // Then
        verify(userRoleChangeService).changeUserRole(eq(1L), eq(List.of(2L, 3L)), anyString());
      }
    }
  }

  @Nested
  @DisplayName("删除用户测试")
  class DeleteUserTests {

    @Test
    @DisplayName("应该成功删除用户")
    void deleteUser_shouldDeleteUser() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(userMapper.deleteById(1L)).thenReturn(1);
      doNothing().when(userMapper).deleteUserRoles(1L);

      // When
      userAppService.deleteUser(1L);

      // Then
      verify(userMapper).deleteById(1L);
      verify(userMapper).deleteUserRoles(1L);
    }

    @Test
    @DisplayName("不能删除admin用户")
    void deleteUser_shouldThrowException_whenDeletingAdmin() {
      // Given
      User adminUser = createTestUser(1L, "admin", "管理员");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(adminUser);

      // When & Then
      assertThatThrownBy(() -> userAppService.deleteUser(1L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("系统管理员不能删除");
    }

    @Test
    @DisplayName("用户不存在时应抛出异常")
    void deleteUser_shouldThrowException_whenUserNotFound() {
      // Given
      when(userRepository.getByIdOrThrow(999L, "用户不存在")).thenThrow(new BusinessException("用户不存在"));

      // When & Then
      assertThatThrownBy(() -> userAppService.deleteUser(999L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("用户不存在");
    }
  }

  @Nested
  @DisplayName("批量删除用户测试")
  class BatchDeleteUsersTests {

    @Test
    @DisplayName("应该成功批量删除用户")
    void deleteUsers_shouldDeleteMultipleUsers() {
      // Given
      User user1 = createTestUser(1L, "user1", "用户1");
      User user2 = createTestUser(2L, "user2", "用户2");
      List<Long> ids = List.of(1L, 2L);

      when(userRepository.findById(1L)).thenReturn(user1);
      when(userRepository.findById(2L)).thenReturn(user2);
      doNothing().when(userMapper).batchDeleteUserRoles(anyList());
      when(userMapper.deleteBatchIds(anyList())).thenReturn(2);

      // When
      UserAppService.BatchDeleteResult result = userAppService.deleteUsers(ids);

      // Then
      assertThat(result.isSuccess()).isTrue();
      assertThat(result.getDeletedCount()).isEqualTo(2);
      verify(userMapper).batchDeleteUserRoles(ids);
      verify(userMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("空ID列表时应抛出异常")
    void deleteUsers_shouldThrowException_whenIdsEmpty() {
      // When & Then
      assertThatThrownBy(() -> userAppService.deleteUsers(new ArrayList<>()))
          .isInstanceOf(BusinessException.class)
          .hasMessage("请选择要删除的用户");
    }

    @Test
    @DisplayName("包含admin用户时应抛出异常")
    void deleteUsers_shouldThrowException_whenContainsAdmin() {
      // Given
      User user1 = createTestUser(1L, "user1", "用户1");
      User adminUser = createTestUser(2L, "admin", "管理员");
      List<Long> ids = List.of(1L, 2L);

      when(userRepository.findById(1L)).thenReturn(user1);
      when(userRepository.findById(2L)).thenReturn(adminUser);

      // When & Then
      assertThatThrownBy(() -> userAppService.deleteUsers(ids))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("系统管理员不能删除");
    }
  }

  @Nested
  @DisplayName("密码管理测试")
  class PasswordManagementTests {

    @Test
    @DisplayName("应该成功重置密码")
    void resetPassword_shouldResetPassword() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
      when(userRepository.updateById(any(User.class))).thenReturn(true);

      // When
      userAppService.resetPassword(1L, "New@Pass123");

      // Then
      verify(userRepository).updateById(any(User.class));
    }

    @Test
    @DisplayName("弱密码应抛出异常")
    void resetPassword_shouldThrowException_forWeakPassword() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);

      // When & Then
      assertThatThrownBy(() -> userAppService.resetPassword(1L, "weak"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("密码长度不能少于8位");
    }

    @Test
    @DisplayName("应该成功修改自己的密码")
    void changePassword_shouldChangePassword() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      user.setPassword("$encoded$old");

      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(passwordEncoder.matches("oldPass", "$encoded$old")).thenReturn(true);
      when(passwordEncoder.encode("New@Pass123")).thenReturn("$encoded$new");
      when(userRepository.updateById(any(User.class))).thenReturn(true);

      // When
      userAppService.changePassword(1L, "oldPass", "New@Pass123");

      // Then
      verify(userRepository).updateById(any(User.class));
    }

    @Test
    @DisplayName("旧密码错误时应抛出异常")
    void changePassword_shouldThrowException_whenOldPasswordIncorrect() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      user.setPassword("$encoded$old");

      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(passwordEncoder.matches("wrongPass", "$encoded$old")).thenReturn(false);

      // When & Then
      assertThatThrownBy(() -> userAppService.changePassword(1L, "wrongPass", "New@Pass123"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("旧密码不正确");
    }
  }

  @Nested
  @DisplayName("用户状态管理测试")
  class StatusManagementTests {

    @Test
    @DisplayName("应该成功修改用户状态")
    void changeStatus_shouldChangeStatus() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(userRepository.updateById(any(User.class))).thenReturn(true);

      // When
      userAppService.changeStatus(1L, "DISABLED");

      // Then
      assertThat(user.getStatus()).isEqualTo("DISABLED");
      verify(userRepository).updateById(user);
    }

    @Test
    @DisplayName("不能禁用admin用户")
    void changeStatus_shouldThrowException_whenDisablingAdmin() {
      // Given
      User adminUser = createTestUser(1L, "admin", "管理员");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(adminUser);

      // When & Then
      assertThatThrownBy(() -> userAppService.changeStatus(1L, "DISABLED"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("系统管理员不能禁用");
    }
  }

  @Nested
  @DisplayName("用户查询测试")
  class UserQueryTests {

    @Test
    @DisplayName("应该通过ID获取用户")
    void getUserById_shouldReturnUser() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(userRepository.findRoleCodesByUserId(1L)).thenReturn(List.of("USER"));
      when(userMapper.selectRoleIdsByUserId(1L)).thenReturn(List.of(1L));
      when(userRepository.findPermissionsByUserId(1L)).thenReturn(List.of("read"));

      // When
      UserDTO result = userAppService.getUserById(1L);

      // Then
      assertThat(result.getUsername()).isEqualTo("testuser");
      assertThat(result.getRoleCodes()).containsExactly("USER");
      assertThat(result.getRoleIds()).containsExactly(1L);
      assertThat(result.getPermissions()).containsExactly("read");
    }

    @Test
    @DisplayName("应该通过用户名获取用户")
    void getUserByUsername_shouldReturnUser() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));

      // When
      UserDTO result = userAppService.getUserByUsername("testuser");

      // Then
      assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("用户名不存在时应抛出异常")
    void getUserByUsername_shouldThrowException_whenUserNotFound() {
      // Given
      when(userRepository.findByUsername("nonexistent")).thenReturn(java.util.Optional.empty());

      // When & Then
      assertThatThrownBy(() -> userAppService.getUserByUsername("nonexistent"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("用户不存在");
    }
  }

  @Nested
  @DisplayName("个人信息管理测试")
  class ProfileManagementTests {

    @Test
    @DisplayName("应该成功更新个人信息")
    void updateProfile_shouldUpdateProfile() {
      // Given
      User user = createTestUser(1L, "testuser", "测试用户");
      when(userRepository.getByIdOrThrow(1L, "用户不存在")).thenReturn(user);
      when(userRepository.updateById(any(User.class))).thenReturn(true);

      // When
      UserDTO result = userAppService.updateProfile(1L, "new@test.com", "13900139000", null);

      // Then
      assertThat(result.getEmail()).isEqualTo("new@test.com");
      assertThat(result.getPhone()).isEqualTo("13900139000");
      verify(userRepository).updateById(user);
    }
  }

  @Nested
  @DisplayName("用户导入导出测试")
  class ImportExportTests {

    @Test
    @DisplayName("应该成功导出用户列表")
    void exportUsers_shouldReturnInputStream() throws Exception {
      // Given
      UserQueryDTO query = new UserQueryDTO();
      query.setPageNum(1);
      query.setPageSize(10000);

      User user = createTestUser(1L, "testuser", "测试用户");
      user.setCompensationType("COMMISSION");
      user.setStatus("ACTIVE");

      Page<User> page = new Page<>(1, 10000);
      page.setRecords(List.of(user));
      page.setTotal(1);

      when(userMapper.selectUserPage(
              ArgumentMatchers.<Page<User>>any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(page);

      ByteArrayInputStream expectedStream = new ByteArrayInputStream("test data".getBytes());
      when(excelImportExportService.createExcel(anyList(), anyList(), anyString()))
          .thenReturn(expectedStream);

      // When
      InputStream result = userAppService.exportUsers(query);

      // Then
      assertThat(result).isNotNull();
      verify(excelImportExportService).createExcel(anyList(), anyList(), anyString());
    }

    @Test
    @DisplayName("应该成功导入用户列表")
    void importUsers_shouldImportUsers() throws Exception {
      // Given
      MultipartFile file = mock(MultipartFile.class);
      List<Map<String, Object>> excelData = new ArrayList<>();

      Map<String, Object> row1 = new HashMap<>();
      row1.put("用户名", "importuser1");
      row1.put("姓名", "导入用户1");
      row1.put("密码", "Valid@Pass123");
      excelData.add(row1);

      when(excelImportExportService.readExcel(file)).thenReturn(excelData);
      when(userRepository.existsByUsername(anyString())).thenReturn(false);
      when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
      when(userRepository.save(any(User.class))).thenReturn(true);

      // When
      Map<String, Object> result = userAppService.importUsers(file);

      // Then
      assertThat(result.get("total")).isEqualTo(1);
      assertThat(result.get("successCount")).isEqualTo(1);
      assertThat(result.get("failCount")).isEqualTo(0);
    }
  }

  // ========== 辅助方法 ==========

  private User createTestUser(Long id, String username, String realName) {
    return User.builder()
        .id(id)
        .username(username)
        .password("$encoded$")
        .realName(realName)
        .email(username + "@test.com")
        .phone("13800138000")
        .departmentId(TEST_DEPARTMENT_ID)
        .position("律师")
        .status("ACTIVE")
        .build();
  }
}
