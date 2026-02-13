package com.lawfirm.application.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.lawfirm.application.system.command.CreateDictItemCommand;
import com.lawfirm.application.system.command.CreateDictTypeCommand;
import com.lawfirm.application.system.dto.DictItemDTO;
import com.lawfirm.application.system.dto.DictTypeDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.common.util.SecurityUtils;
import com.lawfirm.domain.system.entity.DictItem;
import com.lawfirm.domain.system.entity.DictType;
import com.lawfirm.domain.system.repository.DictItemRepository;
import com.lawfirm.domain.system.repository.DictTypeRepository;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
import com.lawfirm.infrastructure.persistence.mapper.DictItemMapper;
import com.lawfirm.infrastructure.persistence.mapper.DictTypeMapper;
import com.lawfirm.infrastructure.security.LoginUser;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DictAppService 单元测试
 *
 * <p>测试数据字典应用服务
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DictAppService 数据字典服务测试")
class DictAppServiceTest {

  @Mock private DictTypeRepository dictTypeRepository;

  @Mock private DictTypeMapper dictTypeMapper;

  @Mock private DictItemRepository dictItemRepository;

  @Mock private DictItemMapper dictItemMapper;

  @Mock private BusinessCacheService businessCacheService;

  @InjectMocks private DictAppService service;

  private LoginUser adminUser;

  @BeforeEach
  void setUp() {
    // 设置管理员用户用于权限测试
    adminUser = new LoginUser();
    adminUser.setUserId(1L);
    adminUser.setUsername("admin");
    adminUser.setRoles(Set.of("ADMIN", "SYSTEM_MANAGER"));
    adminUser.setPermissions(Set.of("sys:dict:update"));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setupAdminUser() {
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(adminUser, null, adminUser.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  // ========== 字典类型测试 ==========

  @Test
  @DisplayName("应该获取所有启用的字典类型")
  void listDictTypes_shouldReturnEnabledTypes() {
    DictType type = createTestDictType();
    when(dictTypeMapper.selectEnabledTypes()).thenReturn(List.of(type));

    List<DictTypeDTO> result = service.listDictTypes();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getCode()).isEqualTo("test_dict");
  }

  @Test
  @DisplayName("应该获取包含字典项的字典类型")
  void getDictTypeWithItems_shouldReturnDictTypeWithItems() {
    DictType type = createTestDictType();
    DictItem item = createTestDictItem();

    when(dictTypeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(type);
    when(dictItemMapper.selectByTypeId(1L)).thenReturn(List.of(item));

    DictTypeDTO result = service.getDictTypeWithItems(1L);

    assertThat(result.getCode()).isEqualTo("test_dict");
    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getLabel()).isEqualTo("测试项");
  }

  @Test
  @DisplayName("应该根据编码获取字典项")
  void getDictItemsByCode_shouldReturnItemsByCode() {
    DictItem item = createTestDictItem();
    when(dictItemMapper.selectByTypeCode("test_dict")).thenReturn(List.of(item));
    when(businessCacheService.getDictItems(eq("test_dict"), any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              java.util.function.Supplier<List<DictItemDTO>> supplier =
                  (java.util.function.Supplier<List<DictItemDTO>>) invocation.getArgument(1);
              return supplier.get();
            });

    List<DictItemDTO> result = service.getDictItemsByCode("test_dict");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLabel()).isEqualTo("测试项");
  }

  @Test
  @DisplayName("创建字典类型时编码不能为空")
  void createDictType_shouldThrowExceptionWhenCodeIsEmpty() {
    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setCode("");
    command.setName("测试字典");

    assertThatThrownBy(() -> service.createDictType(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("字典编码不能为空");
  }

  @Test
  @DisplayName("创建字典类型时编码格式必须正确")
  void createDictType_shouldThrowExceptionWhenCodeFormatIsInvalid() {
    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setCode("123invalid"); // 数字开头
    command.setName("测试字典");

    assertThatThrownBy(() -> service.createDictType(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("字典编码格式错误");
  }

  @Test
  @DisplayName("创建字典类型时编码不能重复")
  void createDictType_shouldThrowExceptionWhenCodeExists() {
    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setCode("existing_dict");
    command.setName("测试字典");

    when(dictTypeMapper.selectByCode("existing_dict")).thenReturn(new DictType());

    assertThatThrownBy(() -> service.createDictType(command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("字典编码已存在");
  }

  @Test
  @DisplayName("应该成功创建字典类型")
  void createDictType_shouldCreateSuccessfully() {
    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setCode("new_dict");
    command.setName("新字典");
    command.setDescription("描述");

    when(dictTypeMapper.selectByCode("new_dict")).thenReturn(null);
    doAnswer(
            invocation -> {
              DictType type = invocation.getArgument(0);
              type.setId(1L);
              return true;
            })
        .when(dictTypeRepository)
        .save(any(DictType.class));

    DictTypeDTO result = service.createDictType(command);

    assertThat(result.getCode()).isEqualTo("new_dict");
    assertThat(result.getName()).isEqualTo("新字典");
  }

  @Test
  @DisplayName("系统内置字典不能修改")
  void updateDictType_shouldThrowExceptionForSystemDict() {
    DictType systemType = createTestDictType();
    systemType.setIsSystem(true);

    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setCode("updated_dict");

    when(dictTypeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(systemType);

    assertThatThrownBy(() -> service.updateDictType(1L, command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("系统内置字典不能修改");
  }

  @Test
  @DisplayName("修改字典编码时如果有字典项应该失败")
  void updateDictType_shouldThrowExceptionWhenHasItems() {
    DictType type = createTestDictType();

    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setCode("updated_dict");

    when(dictTypeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(type);
    when(dictItemMapper.countByTypeId(1L)).thenReturn(5L);

    assertThatThrownBy(() -> service.updateDictType(1L, command))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("无法修改编码");
  }

  @Test
  @DisplayName("应该成功更新字典类型")
  void updateDictType_shouldUpdateSuccessfully() {
    DictType type = createTestDictType();

    CreateDictTypeCommand command = new CreateDictTypeCommand();
    command.setName("更新后的名称");
    command.setDescription("新描述");

    when(dictTypeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(type);

    DictTypeDTO result = service.updateDictType(1L, command);

    assertThat(result.getName()).isEqualTo("更新后的名称");
  }

  @Test
  @DisplayName("系统内置字典不能删除")
  void deleteDictType_shouldThrowExceptionForSystemDict() {
    DictType systemType = createTestDictType();
    systemType.setIsSystem(true);

    when(dictTypeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(systemType);

    assertThatThrownBy(() -> service.deleteDictType(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("系统内置字典不能删除");
  }

  @Test
  @DisplayName("有字典项的字典类型不能删除")
  void deleteDictType_shouldThrowExceptionWhenHasItems() {
    DictType type = createTestDictType();

    when(dictTypeRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(type);
    when(dictItemMapper.countByTypeId(1L)).thenReturn(3L);

    assertThatThrownBy(() -> service.deleteDictType(1L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("无法删除");
  }

  // ========== 字典项测试 ==========

  @Test
  @DisplayName("应该成功创建字典项")
  void createDictItem_shouldCreateSuccessfully() {
    CreateDictItemCommand command = new CreateDictItemCommand();
    command.setDictTypeId(1L);
    command.setLabel("新项");
    command.setValue("new_value");
    command.setDescription("描述");
    command.setSortOrder(10);

    DictType type = createTestDictType();
    when(dictTypeRepository.getByIdOrThrow(any(), anyString())).thenReturn(type);
    doAnswer(
            invocation -> {
              DictItem item = invocation.getArgument(0);
              item.setId(1L);
              return true;
            })
        .when(dictItemRepository)
        .save(any(DictItem.class));

    DictItemDTO result = service.createDictItem(command);

    assertThat(result.getLabel()).isEqualTo("新项");
    assertThat(result.getValue()).isEqualTo("new_value");
    assertThat(result.getSortOrder()).isEqualTo(10);

    // 验证缓存被清除
    verify(businessCacheService).evictDictItems("test_dict");
  }

  @Test
  @DisplayName("应该成功更新字典项")
  void updateDictItem_shouldUpdateSuccessfully() {
    DictItem item = createTestDictItem();
    DictType type = createTestDictType();

    CreateDictItemCommand command = new CreateDictItemCommand();
    command.setLabel("更新后的标签");
    command.setSortOrder(20);

    when(dictItemRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(item);
    when(dictTypeRepository.getById(1L)).thenReturn(type);

    DictItemDTO result = service.updateDictItem(1L, command);

    assertThat(result.getLabel()).isEqualTo("更新后的标签");
    assertThat(result.getSortOrder()).isEqualTo(20);

    // 验证缓存被清除
    verify(businessCacheService).evictDictItems("test_dict");
  }

  @Test
  @DisplayName("删除字典项应该软删除（禁用）")
  void deleteDictItem_shouldSoftDelete() {
    DictItem item = createTestDictItem();
    DictType type = createTestDictType();

    when(dictItemRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(item);
    when(dictTypeRepository.getById(1L)).thenReturn(type);

    service.deleteDictItem(1L);

    assertThat(item.getStatus()).isEqualTo(DictItem.STATUS_DISABLED);
    verify(dictItemRepository).updateById(item);
    verify(businessCacheService).evictDictItems("test_dict");
  }

  @Test
  @DisplayName("非管理员不能切换字典项状态")
  void toggleDictItemStatus_shouldThrowExceptionForNonAdmin() {
    // 设置非管理员用户
    LoginUser normalUser = new LoginUser();
    normalUser.setUserId(2L);
    normalUser.setRoles(Set.of("USER"));

    try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(false);
      mockedSecurity.when(() -> SecurityUtils.hasRole("SYSTEM_MANAGER")).thenReturn(false);

      assertThatThrownBy(() -> service.toggleDictItemStatus(1L))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("权限不足");
    }
  }

  @Test
  @DisplayName("管理员可以切换字典项状态")
  void toggleDictItemStatus_shouldToggleForAdmin() {
    DictItem item = createTestDictItem();
    item.setStatus(DictItem.STATUS_ENABLED);
    DictType type = createTestDictType();

    setupAdminUser();
    when(dictItemRepository.getByIdOrThrow(eq(1L), anyString())).thenReturn(item);
    when(dictTypeRepository.getById(1L)).thenReturn(type);

    try (MockedStatic<SecurityUtils> mockedSecurity = mockStatic(SecurityUtils.class)) {
      mockedSecurity.when(() -> SecurityUtils.hasRole("ADMIN")).thenReturn(true);

      service.toggleDictItemStatus(1L);

      assertThat(item.getStatus()).isEqualTo(DictItem.STATUS_DISABLED);
      verify(businessCacheService).evictDictItems("test_dict");
    }
  }

  // ========== 辅助方法 ==========

  private DictType createTestDictType() {
    return DictType.builder()
        .id(1L)
        .name("测试字典")
        .code("test_dict")
        .description("测试字典描述")
        .status(DictType.STATUS_ENABLED)
        .isSystem(false)
        .build();
  }

  private DictItem createTestDictItem() {
    return DictItem.builder()
        .id(1L)
        .dictTypeId(1L)
        .label("测试项")
        .value("test_value")
        .description("测试项描述")
        .sortOrder(0)
        .status(DictItem.STATUS_ENABLED)
        .cssClass("badge-primary")
        .build();
  }
}
