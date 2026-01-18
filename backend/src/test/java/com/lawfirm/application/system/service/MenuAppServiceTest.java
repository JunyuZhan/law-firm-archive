package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateMenuCommand;
import com.lawfirm.application.system.command.UpdateMenuCommand;
import com.lawfirm.application.system.dto.MenuDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.Menu;
import com.lawfirm.domain.system.repository.MenuRepository;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
import com.lawfirm.infrastructure.persistence.mapper.MenuMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MenuAppService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MenuAppService 菜单服务测试")
class MenuAppServiceTest {

    private static final Long TEST_MENU_ID = 100L;
    private static final Long TEST_ROLE_ID = 200L;
    private static final Long TEST_USER_ID = 1L;

    @Mock
    private MenuRepository menuRepository;

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private BusinessCacheService businessCacheService;

    @InjectMocks
    private MenuAppService menuAppService;

    @Nested
    @DisplayName("查询菜单测试")
    class QueryMenuTests {

        @Test
        @DisplayName("应该成功获取菜单树")
        void getMenuTree_shouldSuccess() {
            // Given
            Menu menu1 = Menu.builder()
                    .id(1L)
                    .parentId(0L)
                    .name("菜单1")
                    .menuType("MENU")
                    .build();

            Menu menu2 = Menu.builder()
                    .id(2L)
                    .parentId(1L)
                    .name("子菜单1")
                    .menuType("MENU")
                    .build();

            when(menuMapper.selectAllMenus()).thenReturn(List.of(menu1, menu2));

            // When
            List<MenuDTO> result = menuAppService.getMenuTree();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("菜单1");
        }

        @Test
        @DisplayName("应该成功获取用户菜单树")
        void getUserMenuTree_shouldSuccess() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .parentId(0L)
                    .name("用户菜单")
                    .menuType("MENU")
                    .build();

            when(businessCacheService.getUserMenus(eq(TEST_USER_ID), any())).thenAnswer(invocation -> {
                when(menuMapper.selectByUserId(TEST_USER_ID)).thenReturn(List.of(menu));
                java.util.function.Supplier<List<MenuDTO>> supplier = invocation.getArgument(1);
                return supplier.get();
            });

            // When
            List<MenuDTO> result = menuAppService.getUserMenuTree(TEST_USER_ID);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("应该成功获取角色菜单ID列表")
        void getRoleMenuIds_shouldSuccess() {
            // Given
            Menu menu1 = Menu.builder().id(1L).build();
            Menu menu2 = Menu.builder().id(2L).build();

            when(menuMapper.selectByRoleId(TEST_ROLE_ID)).thenReturn(List.of(menu1, menu2));

            // When
            List<Long> result = menuAppService.getRoleMenuIds(TEST_ROLE_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).contains(1L, 2L);
        }

        @Test
        @DisplayName("应该成功获取菜单详情")
        void getMenuById_shouldSuccess() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .name("测试菜单")
                    .menuType("MENU")
                    .status(Menu.STATUS_ENABLED)
                    .build();

            when(menuRepository.getByIdOrThrow(eq(TEST_MENU_ID), anyString())).thenReturn(menu);

            // When
            MenuDTO result = menuAppService.getMenuById(TEST_MENU_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("测试菜单");
        }
    }

    @Nested
    @DisplayName("创建菜单测试")
    class CreateMenuTests {

        @Test
        @DisplayName("应该成功创建菜单")
        void createMenu_shouldSuccess() {
            // Given
            CreateMenuCommand command = new CreateMenuCommand();
            command.setName("新菜单");
            command.setPath("/test");
            command.setMenuType("MENU");
            command.setPermission("test:view");

            when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
                Menu menu = invocation.getArgument(0);
                menu.setId(TEST_MENU_ID);
                return true;
            });
            doNothing().when(businessCacheService).evictAllMenus();

            // When
            MenuDTO result = menuAppService.createMenu(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("新菜单");
            assertThat(result.getStatus()).isEqualTo(Menu.STATUS_ENABLED);
            verify(menuRepository).save(any(Menu.class));
            verify(businessCacheService).evictAllMenus();
        }

        @Test
        @DisplayName("应该使用默认值当未指定")
        void createMenu_shouldUseDefaults() {
            // Given
            CreateMenuCommand command = new CreateMenuCommand();
            command.setName("新菜单");
            command.setMenuType("MENU");

            when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> {
                Menu menu = invocation.getArgument(0);
                menu.setId(TEST_MENU_ID);
                return true;
            });
            doNothing().when(businessCacheService).evictAllMenus();

            // When
            menuAppService.createMenu(command);

            // Then
            verify(menuRepository).save(argThat(menu -> menu.getParentId().equals(0L) &&
                    menu.getSortOrder() == 0 &&
                    Boolean.TRUE.equals(menu.getVisible()) &&
                    Boolean.FALSE.equals(menu.getIsExternal()) &&
                    Boolean.TRUE.equals(menu.getIsCache())));
        }
    }

    @Nested
    @DisplayName("更新菜单测试")
    class UpdateMenuTests {

        @Test
        @DisplayName("应该成功更新菜单")
        void updateMenu_shouldSuccess() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .name("原名称")
                    .parentId(0L)
                    .status(Menu.STATUS_ENABLED)
                    .build();

            UpdateMenuCommand command = new UpdateMenuCommand();
            command.setId(TEST_MENU_ID);
            command.setName("新名称");
            command.setPath("/new-path");

            when(menuRepository.getByIdOrThrow(eq(TEST_MENU_ID), anyString())).thenReturn(menu);
            when(menuRepository.getById(anyLong())).thenReturn(null);
            when(menuRepository.updateById(any(Menu.class))).thenReturn(true);
            doNothing().when(businessCacheService).evictAllMenus();

            // When
            MenuDTO result = menuAppService.updateMenu(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(menu.getName()).isEqualTo("新名称");
            verify(menuRepository).updateById(menu);
        }

        @Test
        @DisplayName("应该失败当父菜单是自己")
        void updateMenu_shouldFail_whenParentIsSelf() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .name("测试菜单")
                    .build();

            UpdateMenuCommand command = new UpdateMenuCommand();
            command.setId(TEST_MENU_ID);
            command.setParentId(TEST_MENU_ID);

            when(menuRepository.getByIdOrThrow(eq(TEST_MENU_ID), anyString())).thenReturn(menu);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> menuAppService.updateMenu(command));
            assertThat(exception.getMessage()).contains("父菜单不能是自己");
        }
    }

    @Nested
    @DisplayName("删除菜单测试")
    class DeleteMenuTests {

        @Test
        @DisplayName("应该成功删除菜单")
        void deleteMenu_shouldSuccess() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .name("测试菜单")
                    .build();

            when(menuRepository.getByIdOrThrow(eq(TEST_MENU_ID), anyString())).thenReturn(menu);
            when(menuMapper.selectByParentId(TEST_MENU_ID)).thenReturn(Collections.emptyList());
            when(menuMapper.countRoleMenus(TEST_MENU_ID)).thenReturn(0L);
            when(menuMapper.deleteById(TEST_MENU_ID)).thenReturn(1);
            doNothing().when(businessCacheService).evictAllMenus();

            // When
            menuAppService.deleteMenu(TEST_MENU_ID);

            // Then
            verify(menuMapper).deleteById(TEST_MENU_ID);
            verify(businessCacheService).evictAllMenus();
        }

        @Test
        @DisplayName("应该失败当存在子菜单")
        void deleteMenu_shouldFail_whenHasChildren() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .name("测试菜单")
                    .build();

            Menu childMenu = Menu.builder().id(200L).parentId(TEST_MENU_ID).build();

            when(menuRepository.getByIdOrThrow(eq(TEST_MENU_ID), anyString())).thenReturn(menu);
            when(menuMapper.selectByParentId(TEST_MENU_ID)).thenReturn(List.of(childMenu));

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> menuAppService.deleteMenu(TEST_MENU_ID));
            assertThat(exception.getMessage()).contains("存在子菜单");
        }

        @Test
        @DisplayName("应该失败当菜单已分配给角色")
        void deleteMenu_shouldFail_whenAssignedToRole() {
            // Given
            Menu menu = Menu.builder()
                    .id(TEST_MENU_ID)
                    .name("测试菜单")
                    .build();

            when(menuRepository.getByIdOrThrow(eq(TEST_MENU_ID), anyString())).thenReturn(menu);
            when(menuMapper.selectByParentId(TEST_MENU_ID)).thenReturn(Collections.emptyList());
            when(menuMapper.countRoleMenus(TEST_MENU_ID)).thenReturn(3L);

            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> menuAppService.deleteMenu(TEST_MENU_ID));
            assertThat(exception.getMessage()).contains("已分配给");
        }
    }

    @Nested
    @DisplayName("分配角色菜单测试")
    class AssignRoleMenusTests {

        @Test
        @DisplayName("应该成功分配角色菜单")
        void assignRoleMenus_shouldSuccess() {
            // Given
            List<Long> menuIds = List.of(1L, 2L, 3L);

            when(menuMapper.deleteRoleMenus(TEST_ROLE_ID)).thenReturn(1);
            when(menuMapper.batchInsertRoleMenus(eq(TEST_ROLE_ID), anyList())).thenReturn(1);
            doNothing().when(businessCacheService).evictAllMenus();

            // When
            menuAppService.assignRoleMenus(TEST_ROLE_ID, menuIds);

            // Then
            verify(menuMapper).deleteRoleMenus(TEST_ROLE_ID);
            verify(menuMapper).batchInsertRoleMenus(eq(TEST_ROLE_ID), eq(menuIds));
            verify(businessCacheService).evictAllMenus();
        }

        @Test
        @DisplayName("应该失败当角色ID为空")
        void assignRoleMenus_shouldFail_whenRoleIdNull() {
            // When & Then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> menuAppService.assignRoleMenus(null, List.of(1L)));
            assertThat(exception.getMessage()).contains("角色ID不能为空");
        }

        @Test
        @DisplayName("应该去重菜单ID")
        void assignRoleMenus_shouldDeduplicateMenuIds() {
            // Given
            List<Long> menuIds = List.of(1L, 2L, 2L, 3L, 1L);

            when(menuMapper.deleteRoleMenus(TEST_ROLE_ID)).thenReturn(1);
            when(menuMapper.batchInsertRoleMenus(eq(TEST_ROLE_ID), anyList())).thenReturn(1);
            doNothing().when(businessCacheService).evictAllMenus();

            // When
            menuAppService.assignRoleMenus(TEST_ROLE_ID, menuIds);

            // Then
            verify(menuMapper).batchInsertRoleMenus(eq(TEST_ROLE_ID),
                    argThat(list -> list.size() == 3 && list.containsAll(List.of(1L, 2L, 3L))));
        }
    }
}
