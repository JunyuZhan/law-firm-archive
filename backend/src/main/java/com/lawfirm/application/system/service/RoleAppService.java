package com.lawfirm.application.system.service;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 角色应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAppService {

  /** 角色仓储 */
  private final RoleRepository roleRepository;

  /** 用户角色仓储 */
  private final UserRoleRepository userRoleRepository;

  /** 角色菜单Mapper */
  private final RoleMenuMapper roleMenuMapper;

  /** 菜单Mapper */
  private final MenuMapper menuMapper;

  /** 权限变更日志仓储 */
  private final PermissionChangeLogRepository permissionChangeLogRepository;

  /**
   * 分页查询角色列表
   *
   * @param query 分页查询参数
   * @param keyword 关键词
   * @return 分页结果
   */
  public PageResult<RoleDTO> listRoles(final PageQuery query, final String keyword) {
    Page<Role> page = new Page<>(query.getPageNum(), query.getPageSize());
    LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();

    if (StringUtils.hasText(keyword)) {
      wrapper.like(Role::getRoleName, keyword).or().like(Role::getRoleCode, keyword);
    }
    wrapper.orderByAsc(Role::getSortOrder);

    Page<Role> result = roleRepository.page(page, wrapper);

    List<RoleDTO> items =
        result.getRecords().stream().map(this::toDTO).collect(Collectors.toList());

    return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取所有角色（下拉选择用）
   *
   * @return 角色DTO列表
   */
  public List<RoleDTO> getAllRoles() {
    LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Role::getStatus, "ACTIVE").orderByAsc(Role::getSortOrder);

    return roleRepository.list(wrapper).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /**
   * 获取角色详情
   *
   * @param id 角色ID
   * @return 角色DTO
   */
  public RoleDTO getRoleById(final Long id) {
    Role role = roleRepository.getById(id);
    if (role == null) {
      throw new BusinessException("角色不存在");
    }

    RoleDTO dto = toDTO(role);
    // 查询角色关联的菜单
    dto.setMenuIds(roleMenuMapper.selectMenuIdsByRoleId(id));
    return dto;
  }

  /**
   * 创建角色
   *
   * @param command 创建命令
   * @return 角色DTO
   */
  @Transactional
  public RoleDTO createRole(final CreateRoleCommand command) {
    // 检查角色编码是否存在
    if (roleRepository.existsByRoleCode(command.getRoleCode())) {
      throw new BusinessException("角色编码已存在");
    }

    Role role =
        Role.builder()
            .roleCode(command.getRoleCode())
            .roleName(command.getRoleName())
            .description(command.getDescription())
            .dataScope(command.getDataScope())
            .sortOrder(command.getSortOrder())
            .status("ACTIVE")
            .build();

    roleRepository.save(role);

    // 保存角色菜单关联
    if (command.getMenuIds() != null && !command.getMenuIds().isEmpty()) {
      saveRoleMenus(role.getId(), command.getMenuIds());
    }

    log.info("创建角色成功: {}", role.getRoleCode());
    return toDTO(role);
  }

  /**
   * 更新角色
   *
   * @param command 更新命令
   * @return 角色DTO
   */
  @Transactional
  public RoleDTO updateRole(final UpdateRoleCommand command) {
    Role role = roleRepository.getById(command.getId());
    if (role == null) {
      throw new BusinessException("角色不存在");
    }

    if (StringUtils.hasText(command.getRoleName())) {
      role.setRoleName(command.getRoleName());
    }
    if (command.getDescription() != null) {
      role.setDescription(command.getDescription());
    }
    if (StringUtils.hasText(command.getDataScope())) {
      role.setDataScope(command.getDataScope());
    }
    if (command.getSortOrder() != null) {
      role.setSortOrder(command.getSortOrder());
    }

    roleRepository.updateById(role);

    // 更新角色菜单关联
    if (command.getMenuIds() != null) {
      roleMenuMapper.deleteByRoleId(role.getId());
      if (!command.getMenuIds().isEmpty()) {
        saveRoleMenus(role.getId(), command.getMenuIds());
      }
    }

    log.info("更新角色成功: {}", role.getRoleCode());
    return toDTO(role);
  }

  /**
   * 删除角色
   *
   * @param id 角色ID
   */
  @Transactional
  public void deleteRole(final Long id) {
    Role role = roleRepository.getById(id);
    if (role == null) {
      throw new BusinessException("角色不存在");
    }

    // 检查是否有用户关联
    List<Long> userIds = userRoleRepository.findUserIdsByRoleId(id);
    if (!userIds.isEmpty()) {
      throw new BusinessException("该角色下存在用户，无法删除");
    }

    // 删除角色菜单关联
    roleMenuMapper.deleteByRoleId(id);

    // 删除角色
    roleRepository.removeById(id);

    log.info("删除角色成功: {}", role.getRoleCode());
  }

  /**
   * 修改角色状态
   *
   * @param id 角色ID
   * @param status 状态
   */
  @Transactional
  public void changeStatus(final Long id, final String status) {
    Role role = roleRepository.getById(id);
    if (role == null) {
      throw new BusinessException("角色不存在");
    }

    role.setStatus(status);
    roleRepository.updateById(role);

    log.info("修改角色状态: {} -> {}", role.getRoleCode(), status);
  }

  /**
   * 分配角色菜单 问题454修复：批量查询菜单和批量插入日志 问题455修复：使用差异更新避免先删后插风险
   *
   * @param roleId 角色ID
   * @param menuIds 菜单ID列表
   */
  @Transactional
  public void assignMenus(final Long roleId, final List<Long> menuIds) {
    Role role = roleRepository.getById(roleId);
    if (role == null) {
      throw new BusinessException("角色不存在");
    }

    // 获取原有菜单ID列表
    List<Long> oldMenuIds = roleMenuMapper.selectMenuIdsByRoleId(roleId);
    Set<Long> oldMenuIdSet = new HashSet<>(oldMenuIds);
    Set<Long> newMenuIdSet = menuIds != null ? new HashSet<>(menuIds) : new HashSet<>();

    // 计算新增和移除的权限
    Set<Long> addedMenuIds = new HashSet<>(newMenuIdSet);
    addedMenuIds.removeAll(oldMenuIdSet);

    Set<Long> removedMenuIds = new HashSet<>(oldMenuIdSet);
    removedMenuIds.removeAll(newMenuIdSet);

    // 问题455修复：使用差异更新
    // 先添加新的菜单关联
    if (!addedMenuIds.isEmpty()) {
      saveRoleMenus(roleId, new ArrayList<>(addedMenuIds));
    }

    // 再删除旧的菜单关联
    if (!removedMenuIds.isEmpty()) {
      roleMenuMapper.deleteByRoleIdAndMenuIds(roleId, new ArrayList<>(removedMenuIds));
    }

    // 问题454修复：批量查询菜单信息
    Set<Long> allMenuIds = new HashSet<>();
    allMenuIds.addAll(addedMenuIds);
    allMenuIds.addAll(removedMenuIds);

    if (!allMenuIds.isEmpty()) {
      Map<Long, Menu> menuMap =
          menuMapper.selectBatchIds(new ArrayList<>(allMenuIds)).stream()
              .collect(Collectors.toMap(Menu::getId, m -> m));

      // 记录权限变更历史
      Long changedBy = SecurityUtils.getUserId();
      LocalDateTime changedAt = LocalDateTime.now();
      List<PermissionChangeLog> changeLogs = new ArrayList<>();

      // 记录新增的权限
      for (Long menuId : addedMenuIds) {
        Menu menu = menuMap.get(menuId);
        if (menu != null && menu.getPermission() != null && !menu.getPermission().isEmpty()) {
          PermissionChangeLog changeLog =
              PermissionChangeLog.builder()
                  .roleId(roleId)
                  .roleCode(role.getRoleCode())
                  .changeType("ADD")
                  .permissionCode(menu.getPermission())
                  .permissionName(menu.getName())
                  .changedBy(changedBy)
                  .changedAt(changedAt)
                  .build();
          changeLogs.add(changeLog);
        }
      }

      // 记录移除的权限
      for (Long menuId : removedMenuIds) {
        Menu menu = menuMap.get(menuId);
        if (menu != null && menu.getPermission() != null && !menu.getPermission().isEmpty()) {
          PermissionChangeLog changeLog =
              PermissionChangeLog.builder()
                  .roleId(roleId)
                  .roleCode(role.getRoleCode())
                  .changeType("REMOVE")
                  .permissionCode(menu.getPermission())
                  .permissionName(menu.getName())
                  .changedBy(changedBy)
                  .changedAt(changedAt)
                  .build();
          changeLogs.add(changeLog);
        }
      }

      // 问题454修复：批量插入日志
      if (!changeLogs.isEmpty()) {
        permissionChangeLogRepository.saveBatch(changeLogs);
      }
    }

    log.info(
        "分配角色菜单成功: roleId={}, menuCount={}, added={}, removed={}",
        roleId,
        menuIds != null ? menuIds.size() : 0,
        addedMenuIds.size(),
        removedMenuIds.size());
  }

  /**
   * 获取角色的菜单ID列表
   *
   * @param roleId 角色ID
   * @return 菜单ID列表
   */
  public List<Long> getRoleMenuIds(final Long roleId) {
    return roleMenuMapper.selectMenuIdsByRoleId(roleId);
  }

  /**
   * 保存角色菜单关联 问题453修复：使用批量插入替代循环插入
   *
   * @param roleId 角色ID
   * @param menuIds 菜单ID列表
   */
  private void saveRoleMenus(final Long roleId, final List<Long> menuIds) {
    if (menuIds == null || menuIds.isEmpty()) {
      return;
    }

    List<RoleMenu> roleMenus =
        menuIds.stream()
            .map(menuId -> RoleMenu.builder().roleId(roleId).menuId(menuId).build())
            .collect(Collectors.toList());

    // 问题453修复：批量插入
    roleMenuMapper.insertBatch(roleMenus);
  }

  /**
   * 转换为DTO
   *
   * @param role 角色实体
   * @return 角色DTO
   */
  private RoleDTO toDTO(final Role role) {
    return RoleDTO.builder()
        .id(role.getId())
        .roleCode(role.getRoleCode())
        .roleName(role.getRoleName())
        .description(role.getDescription())
        .dataScope(role.getDataScope())
        .status(role.getStatus())
        .sortOrder(role.getSortOrder())
        .createdAt(role.getCreatedAt())
        .updatedAt(role.getUpdatedAt())
        .build();
  }
}
