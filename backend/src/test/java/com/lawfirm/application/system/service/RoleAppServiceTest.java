package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.application.system.command.CreateRoleCommand;
import com.lawfirm.application.system.command.UpdateRoleCommand;
import com.lawfirm.application.system.dto.RoleDTO;
import com.lawfirm.common.base.PageQuery;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.Menu;
import com.lawfirm.domain.system.entity.PermissionChangeLog;
import com.lawfirm.domain.system.entity.Role;
import com.lawfirm.domain.system.entity.RoleMenu;
import com.lawfirm.domain.system.repository.PermissionChangeLogRepository;
import com.lawfirm.domain.system.repository.RoleRepository;
import com.lawfirm.domain.system.repository.UserRoleRepository;
import com.lawfirm.infrastructure.persistence.mapper.MenuMapper;
import com.lawfirm.infrastructure.persistence.mapper.RoleMenuMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleAppService 单元测试
 *
 * <p>测试角色应用服务的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleAppService 角色服务测试")
class RoleAppServiceTest {

  private static final Long TEST_USER_ID = 1L;

  @Mock private RoleRepository roleRepository;

  @Mock private UserRoleRepository userRoleRepository;

  @Mock private RoleMenuMapper roleMenuMapper;

  @Mock private MenuMapper menuMapper;

  @Mock private PermissionChangeLogRepository permissionChangeLogRepository;

  @InjectMocks private RoleAppService roleAppService;

  @Nested
  @DisplayName("分页查询角色测试")
  class ListRolesTests {

    @Test
    @DisplayName("应该分页查询角色列表")
    void listRoles_shouldReturnPagedResult() {
      // Given
      PageQuery query = new PageQuery();
      query.setPageNum(1);
      query.setPageSize(10);

      Role role = createTestRole(1L, "ROLE_USER", "普通用户");
      Page<Role> page = new Page<>(1, 10);
      page.setRecords(List.of(role));
      page.setTotal(1);

      when(roleRepository.page(
              ArgumentMatchers.<Page<Role>>any(), ArgumentMatchers.<LambdaQueryWrapper<Role>>any()))
          .thenReturn(page);

      // When
      PageResult<RoleDTO> result = roleAppService.listRoles(query, null);

      // Then
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getRoleCode()).isEqualTo("ROLE_USER");
      assertThat(result.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("应该支持关键词搜索")
    void listRoles_shouldSupportKeywordSearch() {
      // Given
      PageQuery query = new PageQuery();
      query.setPageNum(1);
      query.setPageSize(10);

      Role role = createTestRole(1L, "ROLE_ADMIN", "管理员");
      Page<Role> page = new Page<>(1, 10);
      page.setRecords(List.of(role));
      page.setTotal(1);

      when(roleRepository.page(
              ArgumentMatchers.<Page<Role>>any(), ArgumentMatchers.<LambdaQueryWrapper<Role>>any()))
          .thenReturn(page);

      // When
      PageResult<RoleDTO> result = roleAppService.listRoles(query, "管理员");

      // Then
      assertThat(result.getRecords()).hasSize(1);
      assertThat(result.getRecords().get(0).getRoleName()).isEqualTo("管理员");
    }

    @Test
    @DisplayName("空结果时应返回空分页")
    void listRoles_shouldReturnEmptyWhenNoResults() {
      // Given
      PageQuery query = new PageQuery();
      query.setPageNum(1);
      query.setPageSize(10);

      Page<Role> page = new Page<>(1, 10);
      page.setRecords(new ArrayList<>());
      page.setTotal(0);

      when(roleRepository.page(
              ArgumentMatchers.<Page<Role>>any(), ArgumentMatchers.<LambdaQueryWrapper<Role>>any()))
          .thenReturn(page);

      // When
      PageResult<RoleDTO> result = roleAppService.listRoles(query, null);

      // Then
      assertThat(result.getRecords()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("获取所有角色测试")
  class GetAllRolesTests {

    @Test
    @DisplayName("应该获取所有激活角色")
    void getAllRoles_shouldReturnActiveRoles() {
      // Given
      Role role1 = createTestRole(1L, "ROLE_USER", "普通用户");
      role1.setStatus("ACTIVE");
      Role role2 = createTestRole(2L, "ROLE_ADMIN", "管理员");
      role2.setStatus("ACTIVE");

      when(roleRepository.list(ArgumentMatchers.<LambdaQueryWrapper<Role>>any()))
          .thenReturn(List.of(role1, role2));

      // When
      List<RoleDTO> result = roleAppService.getAllRoles();

      // Then
      assertThat(result).hasSize(2);
      assertThat(result.get(0).getRoleCode()).isEqualTo("ROLE_USER");
      assertThat(result.get(1).getRoleCode()).isEqualTo("ROLE_ADMIN");
    }
  }

  @Nested
  @DisplayName("获取角色详情测试")
  class GetRoleByIdTests {

    @Test
    @DisplayName("应该获取角色详情")
    void getRoleById_shouldReturnRoleWithMenus() {
      // Given
      Role role = createTestRole(1L, "ROLE_USER", "普通用户");
      when(roleRepository.getById(1L)).thenReturn(role);
      when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(100L, 200L));

      // When
      RoleDTO result = roleAppService.getRoleById(1L);

      // Then
      assertThat(result.getRoleCode()).isEqualTo("ROLE_USER");
      assertThat(result.getMenuIds()).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("角色不存在时应抛出异常")
    void getRoleById_shouldThrowException_whenRoleNotFound() {
      // Given
      when(roleRepository.getById(999L)).thenReturn(null);

      // When & Then
      assertThatThrownBy(() -> roleAppService.getRoleById(999L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("角色不存在");
    }
  }

  @Nested
  @DisplayName("创建角色测试")
  class CreateRoleTests {

    @Test
    @DisplayName("应该成功创建角色")
    void createRole_shouldReturnRoleDTO() {
      // Given
      CreateRoleCommand command = new CreateRoleCommand();
      command.setRoleCode("ROLE_NEW");
      command.setRoleName("新角色");
      command.setDescription("新角色描述");
      command.setDataScope("ALL");
      command.setSortOrder(1);
      command.setMenuIds(List.of(100L, 200L));

      when(roleRepository.existsByRoleCode("ROLE_NEW")).thenReturn(false);
      when(roleRepository.save(any(Role.class))).thenReturn(true);
      doNothing().when(roleMenuMapper).insertBatch(anyList());

      // When
      RoleDTO result = roleAppService.createRole(command);

      // Then
      assertThat(result.getRoleCode()).isEqualTo("ROLE_NEW");
      assertThat(result.getRoleName()).isEqualTo("新角色");
      assertThat(result.getStatus()).isEqualTo("ACTIVE");
      verify(roleRepository).save(any(Role.class));
      verify(roleMenuMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("角色编码已存在时应抛出异常")
    void createRole_shouldThrowException_whenRoleCodeExists() {
      // Given
      CreateRoleCommand command = new CreateRoleCommand();
      command.setRoleCode("ROLE_EXISTS");
      command.setRoleName("已存在角色");

      when(roleRepository.existsByRoleCode("ROLE_EXISTS")).thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> roleAppService.createRole(command))
          .isInstanceOf(BusinessException.class)
          .hasMessage("角色编码已存在");
    }

    @Test
    @DisplayName("不分配菜单时不应插入菜单关联")
    void createRole_shouldNotInsertMenus_whenNoMenusAssigned() {
      // Given
      CreateRoleCommand command = new CreateRoleCommand();
      command.setRoleCode("ROLE_NEW");
      command.setRoleName("新角色");
      command.setMenuIds(null);

      when(roleRepository.existsByRoleCode("ROLE_NEW")).thenReturn(false);
      when(roleRepository.save(any(Role.class))).thenReturn(true);

      // When
      RoleDTO result = roleAppService.createRole(command);

      // Then
      assertThat(result.getRoleCode()).isEqualTo("ROLE_NEW");
      verify(roleMenuMapper, never()).insertBatch(anyList());
    }
  }

  @Nested
  @DisplayName("更新角色测试")
  class UpdateRoleTests {

    @Test
    @DisplayName("应该成功更新角色")
    void updateRole_shouldReturnRoleDTO() {
      // Given
      Role existingRole = createTestRole(1L, "ROLE_USER", "普通用户");
      existingRole.setDataScope("DEPT");

      UpdateRoleCommand command = new UpdateRoleCommand();
      command.setId(1L);
      command.setRoleName("更新后的用户");
      command.setDescription("更新描述");
      command.setDataScope("ALL");
      command.setSortOrder(10);
      command.setMenuIds(List.of(100L));

      when(roleRepository.getById(1L)).thenReturn(existingRole);
      when(roleRepository.updateById(any(Role.class))).thenReturn(true);
      when(roleMenuMapper.deleteByRoleId(1L)).thenReturn(2);
      doNothing().when(roleMenuMapper).insertBatch(anyList());

      // When
      RoleDTO result = roleAppService.updateRole(command);

      // Then
      assertThat(result.getRoleName()).isEqualTo("更新后的用户");
      assertThat(result.getDataScope()).isEqualTo("ALL");
      verify(roleRepository).updateById(any(Role.class));
    }

    @Test
    @DisplayName("角色不存在时应抛出异常")
    void updateRole_shouldThrowException_whenRoleNotFound() {
      // Given
      UpdateRoleCommand command = new UpdateRoleCommand();
      command.setId(999L);

      when(roleRepository.getById(999L)).thenReturn(null);

      // When & Then
      assertThatThrownBy(() -> roleAppService.updateRole(command))
          .isInstanceOf(BusinessException.class)
          .hasMessage("角色不存在");
    }

    @Test
    @DisplayName("清空菜单时应删除所有菜单关联")
    void updateRole_shouldDeleteAllMenus_whenMenuIdsEmpty() {
      // Given
      Role existingRole = createTestRole(1L, "ROLE_USER", "普通用户");

      UpdateRoleCommand command = new UpdateRoleCommand();
      command.setId(1L);
      command.setMenuIds(new ArrayList<>());

      when(roleRepository.getById(1L)).thenReturn(existingRole);
      when(roleRepository.updateById(any(Role.class))).thenReturn(true);
      when(roleMenuMapper.deleteByRoleId(1L)).thenReturn(2);

      // When
      roleAppService.updateRole(command);

      // Then
      verify(roleMenuMapper).deleteByRoleId(1L);
      verify(roleMenuMapper, never()).insertBatch(anyList());
    }
  }

  @Nested
  @DisplayName("删除角色测试")
  class DeleteRoleTests {

    @Test
    @DisplayName("应该成功删除角色")
    void deleteRole_shouldDeleteRole() {
      // Given
      Role role = createTestRole(1L, "ROLE_USER", "普通用户");
      when(roleRepository.getById(1L)).thenReturn(role);
      when(userRoleRepository.findUserIdsByRoleId(1L)).thenReturn(new ArrayList<>());
      when(roleMenuMapper.deleteByRoleId(1L)).thenReturn(2);
      when(roleRepository.removeById(1L)).thenReturn(true);

      // When
      roleAppService.deleteRole(1L);

      // Then
      verify(roleMenuMapper).deleteByRoleId(1L);
      verify(roleRepository).removeById(1L);
    }

    @Test
    @DisplayName("角色不存在时应抛出异常")
    void deleteRole_shouldThrowException_whenRoleNotFound() {
      // Given
      when(roleRepository.getById(999L)).thenReturn(null);

      // When & Then
      assertThatThrownBy(() -> roleAppService.deleteRole(999L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("角色不存在");
    }

    @Test
    @DisplayName("有关联用户时应抛出异常")
    void deleteRole_shouldThrowException_whenHasUsers() {
      // Given
      Role role = createTestRole(1L, "ROLE_USER", "普通用户");
      when(roleRepository.getById(1L)).thenReturn(role);
      when(userRoleRepository.findUserIdsByRoleId(1L)).thenReturn(List.of(100L, 200L));

      // When & Then
      assertThatThrownBy(() -> roleAppService.deleteRole(1L))
          .isInstanceOf(BusinessException.class)
          .hasMessage("该角色下存在用户，无法删除");
    }
  }

  @Nested
  @DisplayName("角色状态管理测试")
  class StatusManagementTests {

    @Test
    @DisplayName("应该成功修改角色状态")
    void changeStatus_shouldChangeStatus() {
      // Given
      Role role = createTestRole(1L, "ROLE_USER", "普通用户");
      role.setStatus("ACTIVE");

      when(roleRepository.getById(1L)).thenReturn(role);
      when(roleRepository.updateById(any(Role.class))).thenReturn(true);

      // When
      roleAppService.changeStatus(1L, "DISABLED");

      // Then
      assertThat(role.getStatus()).isEqualTo("DISABLED");
      verify(roleRepository).updateById(role);
    }

    @Test
    @DisplayName("角色不存在时应抛出异常")
    void changeStatus_shouldThrowException_whenRoleNotFound() {
      // Given
      when(roleRepository.getById(999L)).thenReturn(null);

      // When & Then
      assertThatThrownBy(() -> roleAppService.changeStatus(999L, "DISABLED"))
          .isInstanceOf(BusinessException.class)
          .hasMessage("角色不存在");
    }
  }

  @Nested
  @DisplayName("分配菜单测试")
  class AssignMenusTests {

    @Test
    @DisplayName("应该成功分配菜单")
    void assignMenus_shouldAssignMenus() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        Role role = createTestRole(1L, "ROLE_USER", "普通用户");

        Menu menu1 = createTestMenu(100L, "用户管理", "user:read");
        Menu menu2 = createTestMenu(200L, "角色管理", "role:read");

        when(roleRepository.getById(1L)).thenReturn(role);
        when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(new ArrayList<>());
        when(menuMapper.selectBatchIds(anyList())).thenReturn(List.of(menu1, menu2));
        doNothing().when(roleMenuMapper).insertBatch(anyList());
        when(permissionChangeLogRepository.saveBatch(anyList())).thenReturn(true);

        // When
        roleAppService.assignMenus(1L, List.of(100L, 200L));

        // Then
        verify(roleMenuMapper).insertBatch(anyList());
        verify(permissionChangeLogRepository).saveBatch(anyList());
      }
    }

    @Test
    @DisplayName("应该使用差异更新避免重复插入")
    void assignMenus_shouldUseDifferentialUpdate() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        Role role = createTestRole(1L, "ROLE_USER", "普通用户");

        Menu menu2 = createTestMenu(200L, "角色管理", "role:read");

        when(roleRepository.getById(1L)).thenReturn(role);
        // 已有菜单100
        when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(100L));
        when(menuMapper.selectBatchIds(anyList())).thenReturn(List.of(menu2));
        doNothing().when(roleMenuMapper).insertBatch(anyList());
        when(permissionChangeLogRepository.saveBatch(anyList())).thenReturn(true);

        // When - 分配菜单100和200，100已存在，只需新增200
        roleAppService.assignMenus(1L, List.of(100L, 200L));

        // Then - 只插入菜单200
        verify(roleMenuMapper)
            .insertBatch(
                argThat(
                    list -> list.size() == 1 && ((RoleMenu) list.get(0)).getMenuId().equals(200L)));
      }
    }

    @Test
    @DisplayName("角色不存在时应抛出异常")
    void assignMenus_shouldThrowException_whenRoleNotFound() {
      // Given
      when(roleRepository.getById(999L)).thenReturn(null);

      // When & Then
      assertThatThrownBy(() -> roleAppService.assignMenus(999L, List.of(100L)))
          .isInstanceOf(BusinessException.class)
          .hasMessage("角色不存在");
    }

    @Test
    @DisplayName("应该记录权限变更历史")
    void assignMenus_shouldLogPermissionChanges() {
      try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
        // Given
        mockedSecurity.when(SecurityUtils::getUserId).thenReturn(TEST_USER_ID);

        Role role = createTestRole(1L, "ROLE_USER", "普通用户");

        Menu menu = createTestMenu(100L, "用户管理", "user:read");

        when(roleRepository.getById(1L)).thenReturn(role);
        when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(new ArrayList<>());
        when(menuMapper.selectBatchIds(anyList())).thenReturn(List.of(menu));
        doNothing().when(roleMenuMapper).insertBatch(anyList());
        when(permissionChangeLogRepository.saveBatch(anyList())).thenReturn(true);

        // When
        roleAppService.assignMenus(1L, List.of(100L));

        // Then
        verify(permissionChangeLogRepository)
            .saveBatch(
                argThat(
                    collection -> {
                      List<PermissionChangeLog> list = new ArrayList<>(collection);
                      return !list.isEmpty()
                          && list.get(0).getChangeType().equals("ADD")
                          && list.get(0).getPermissionCode().equals("user:read");
                    }));
      }
    }
  }

  @Nested
  @DisplayName("获取角色菜单测试")
  class GetRoleMenuIdsTests {

    @Test
    @DisplayName("应该获取角色菜单ID列表")
    void getRoleMenuIds_shouldReturnMenuIds() {
      // Given
      when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(List.of(100L, 200L, 300L));

      // When
      List<Long> result = roleAppService.getRoleMenuIds(1L);

      // Then
      assertThat(result).containsExactly(100L, 200L, 300L);
    }

    @Test
    @DisplayName("无菜单时应返回空列表")
    void getRoleMenuIds_shouldReturnEmptyWhenNoMenus() {
      // Given
      when(roleMenuMapper.selectMenuIdsByRoleId(1L)).thenReturn(new ArrayList<>());

      // When
      List<Long> result = roleAppService.getRoleMenuIds(1L);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ========== 辅助方法 ==========

  private Role createTestRole(Long id, String roleCode, String roleName) {
    return Role.builder()
        .id(id)
        .roleCode(roleCode)
        .roleName(roleName)
        .description("测试角色")
        .dataScope("DEPT")
        .status("ACTIVE")
        .sortOrder(1)
        .build();
  }

  private Menu createTestMenu(Long id, String name, String permission) {
    return Menu.builder().id(id).name(name).permission(permission).build();
  }
}
