package com.lawfirm.application.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lawfirm.application.system.dto.RoleDTO;
import com.lawfirm.domain.system.entity.Menu;
import com.lawfirm.domain.system.entity.Role;
import com.lawfirm.domain.system.repository.RoleRepository;
import com.lawfirm.infrastructure.persistence.mapper.MenuMapper;
import com.lawfirm.infrastructure.persistence.mapper.RoleMenuMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 权限矩阵服务. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionMatrixService {

  /** 角色仓储. */
  private final RoleRepository roleRepository;

  /** 菜单Mapper. */
  private final MenuMapper menuMapper;

  /** 角色菜单Mapper. */
  private final RoleMenuMapper roleMenuMapper;

  /**
   * 获取权限矩阵
   *
   * @param module 模块筛选（可选，如：client, matter, finance）
   * @param permissionType 权限类型筛选（可选，如：MENU, BUTTON）
   * @return 权限矩阵数据
   */
  public PermissionMatrixDTO getPermissionMatrix(final String module, final String permissionType) {
    // 1. 获取所有角色
    List<Role> allRoles =
        roleRepository.list(
            new LambdaQueryWrapper<Role>()
                .eq(Role::getDeleted, false)
                .orderByAsc(Role::getSortOrder));

    // 2. 获取所有菜单（权限）
    List<Menu> allMenus = menuMapper.selectAllMenus();

    // 3. 筛选菜单
    List<Menu> filteredMenus =
        allMenus.stream()
            .filter(
                menu -> {
                  // 按模块筛选
                  if (module != null && !module.isEmpty()) {
                    String permission = menu.getPermission();
                    if (permission == null || !permission.startsWith(module + ":")) {
                      return false;
                    }
                  }
                  // 按权限类型筛选
                  if (permissionType != null && !permissionType.isEmpty()) {
                    if (!permissionType.equals(menu.getMenuType())) {
                      return false;
                    }
                  }
                  // 只返回有权限码的菜单（BUTTON类型或MENU类型）
                  return menu.getPermission() != null && !menu.getPermission().isEmpty();
                })
            .sorted(Comparator.comparing(Menu::getSortOrder).thenComparing(Menu::getId))
            .collect(Collectors.toList());

    // 4. 获取每个角色的菜单ID列表
    Map<Long, Set<Long>> roleMenuMap = new HashMap<>();
    for (Role role : allRoles) {
      List<Long> menuIds = roleMenuMapper.selectMenuIdsByRoleId(role.getId());
      roleMenuMap.put(role.getId(), new HashSet<>(menuIds));
    }

    // 5. 构建权限矩阵
    List<PermissionMatrixRowDTO> rows = new ArrayList<>();
    for (Role role : allRoles) {
      PermissionMatrixRowDTO row = new PermissionMatrixRowDTO();
      row.setRoleId(role.getId());
      row.setRoleCode(role.getRoleCode());
      row.setRoleName(role.getRoleName());
      row.setDataScope(role.getDataScope());

      // 构建权限列表
      Set<Long> roleMenuIds = roleMenuMap.getOrDefault(role.getId(), Collections.emptySet());
      List<PermissionMatrixCellDTO> cells = new ArrayList<>();
      for (Menu menu : filteredMenus) {
        PermissionMatrixCellDTO cell = new PermissionMatrixCellDTO();
        cell.setPermissionCode(menu.getPermission());
        cell.setPermissionName(menu.getName());
        cell.setMenuType(menu.getMenuType());
        cell.setHasPermission(roleMenuIds.contains(menu.getId()));
        cells.add(cell);
      }
      row.setPermissions(cells);
      rows.add(row);
    }

    // 6. 构建返回结果
    PermissionMatrixDTO result = new PermissionMatrixDTO();
    result.setRoles(
        allRoles.stream()
            .map(
                r -> {
                  RoleDTO dto = new RoleDTO();
                  dto.setId(r.getId());
                  dto.setRoleCode(r.getRoleCode());
                  dto.setRoleName(r.getRoleName());
                  dto.setDataScope(r.getDataScope());
                  return dto;
                })
            .collect(Collectors.toList()));
    result.setPermissions(
        filteredMenus.stream()
            .map(
                m -> {
                  PermissionDTO dto = new PermissionDTO();
                  dto.setPermissionCode(m.getPermission());
                  dto.setPermissionName(m.getName());
                  dto.setMenuType(m.getMenuType());
                  dto.setModule(extractModule(m.getPermission()));
                  return dto;
                })
            .collect(Collectors.toList()));
    result.setMatrix(rows);

    return result;
  }

  /**
   * 获取角色权限详情
   *
   * @param roleId 角色ID
   * @return 角色权限DTO
   */
  public RolePermissionDTO getRolePermissions(final Long roleId) {
    Role role = roleRepository.getById(roleId);
    if (role == null) {
      throw new com.lawfirm.common.exception.BusinessException("角色不存在");
    }

    // 获取角色的菜单列表
    List<Menu> roleMenus = menuMapper.selectByRoleId(roleId);

    // 构建权限列表
    List<PermissionDTO> permissions =
        roleMenus.stream()
            .filter(menu -> menu.getPermission() != null && !menu.getPermission().isEmpty())
            .map(
                menu -> {
                  PermissionDTO dto = new PermissionDTO();
                  dto.setPermissionCode(menu.getPermission());
                  dto.setPermissionName(menu.getName());
                  dto.setMenuType(menu.getMenuType());
                  dto.setModule(extractModule(menu.getPermission()));
                  return dto;
                })
            .sorted(
                Comparator.comparing(PermissionDTO::getModule)
                    .thenComparing(PermissionDTO::getPermissionCode))
            .collect(Collectors.toList());

    RolePermissionDTO result = new RolePermissionDTO();
    result.setRoleId(role.getId());
    result.setRoleCode(role.getRoleCode());
    result.setRoleName(role.getRoleName());
    result.setDataScope(role.getDataScope());
    result.setPermissions(permissions);

    return result;
  }

  /**
   * 对比角色权限
   *
   * @param roleIds 角色ID列表
   * @return 权限对比DTO
   */
  public PermissionCompareDTO comparePermissions(final List<Long> roleIds) {
    if (roleIds == null || roleIds.isEmpty()) {
      throw new com.lawfirm.common.exception.BusinessException("至少需要选择一个角色进行对比");
    }

    // 获取所有权限
    List<Menu> allMenus = menuMapper.selectAllMenus();
    List<Menu> permissionMenus =
        allMenus.stream()
            .filter(menu -> menu.getPermission() != null && !menu.getPermission().isEmpty())
            .sorted(Comparator.comparing(Menu::getPermission))
            .collect(Collectors.toList());

    // 获取每个角色的权限
    Map<Long, Set<String>> rolePermissionMap = new HashMap<>();
    for (Long roleId : roleIds) {
      List<Menu> roleMenus = menuMapper.selectByRoleId(roleId);
      Set<String> permissions =
          roleMenus.stream()
              .filter(menu -> menu.getPermission() != null && !menu.getPermission().isEmpty())
              .map(Menu::getPermission)
              .collect(Collectors.toSet());
      rolePermissionMap.put(roleId, permissions);
    }

    // 构建对比结果
    List<PermissionCompareRowDTO> rows = new ArrayList<>();
    for (Menu menu : permissionMenus) {
      PermissionCompareRowDTO row = new PermissionCompareRowDTO();
      row.setPermissionCode(menu.getPermission());
      row.setPermissionName(menu.getName());
      row.setMenuType(menu.getMenuType());
      row.setModule(extractModule(menu.getPermission()));

      // 检查每个角色是否有该权限
      Map<Long, Boolean> roleHasPermission = new HashMap<>();
      for (Long roleId : roleIds) {
        Set<String> permissions = rolePermissionMap.get(roleId);
        roleHasPermission.put(
            roleId, permissions != null && permissions.contains(menu.getPermission()));
      }
      row.setRoleHasPermission(roleHasPermission);
      rows.add(row);
    }

    PermissionCompareDTO result = new PermissionCompareDTO();
    result.setRoleIds(roleIds);
    result.setRoles(
        roleIds.stream()
            .map(
                roleId -> {
                  Role role = roleRepository.getById(roleId);
                  if (role == null) {
                    return null;
                  }
                  RoleDTO dto = new RoleDTO();
                  dto.setId(role.getId());
                  dto.setRoleCode(role.getRoleCode());
                  dto.setRoleName(role.getRoleName());
                  return dto;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList()));
    result.setPermissions(rows);

    return result;
  }

  /**
   * 从权限码中提取模块名 例如：client:list -> client
   *
   * @param permissionCode 权限码
   * @return 模块名
   */
  private String extractModule(final String permissionCode) {
    if (permissionCode == null || permissionCode.isEmpty()) {
      return null;
    }
    int colonIndex = permissionCode.indexOf(':');
    if (colonIndex > 0) {
      return permissionCode.substring(0, colonIndex);
    }
    return permissionCode;
  }

  // ========== DTOs ==========

  /** 权限矩阵DTO. */
  @Data
  public static class PermissionMatrixDTO {
    /** 角色列表. */
    private List<RoleDTO> roles;

    /** 权限列表. */
    private List<PermissionDTO> permissions;

    /** 权限矩阵行列表. */
    private List<PermissionMatrixRowDTO> matrix;
  }

  /** 权限矩阵行DTO. */
  @Data
  public static class PermissionMatrixRowDTO {
    /** 角色ID. */
    private Long roleId;

    /** 角色代码. */
    private String roleCode;

    /** 角色名称. */
    private String roleName;

    /** 数据权限范围. */
    private String dataScope;

    /** 权限单元格列表. */
    private List<PermissionMatrixCellDTO> permissions;
  }

  /** 权限矩阵单元格DTO. */
  @Data
  public static class PermissionMatrixCellDTO {
    /** 权限代码. */
    private String permissionCode;

    /** 权限名称. */
    private String permissionName;

    /** 菜单类型. */
    private String menuType;

    /** 是否拥有权限. */
    private Boolean hasPermission;
  }

  /** 权限DTO. */
  @Data
  public static class PermissionDTO {
    /** 权限代码. */
    private String permissionCode;

    /** 权限名称. */
    private String permissionName;

    /** 菜单类型. */
    private String menuType;

    /** 模块名称. */
    private String module;
  }

  /** 角色权限DTO. */
  @Data
  public static class RolePermissionDTO {
    /** 角色ID. */
    private Long roleId;

    /** 角色代码. */
    private String roleCode;

    /** 角色名称. */
    private String roleName;

    /** 数据权限范围. */
    private String dataScope;

    /** 权限列表. */
    private List<PermissionDTO> permissions;
  }

  /** 权限比较DTO. */
  @Data
  public static class PermissionCompareDTO {
    /** 角色ID列表. */
    private List<Long> roleIds;

    /** 角色列表. */
    private List<RoleDTO> roles;

    /** 权限比较行列表. */
    private List<PermissionCompareRowDTO> permissions;
  }

  /** 权限比较行DTO. */
  @Data
  public static class PermissionCompareRowDTO {
    /** 权限代码. */
    private String permissionCode;

    /** 权限名称. */
    private String permissionName;

    /** 菜单类型. */
    private String menuType;

    /** 模块名称. */
    private String module;

    /** 角色权限映射（key: roleId, value: hasPermission）. */
    private Map<Long, Boolean> roleHasPermission;
  }
}
