package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateMenuCommand;
import com.lawfirm.application.system.command.UpdateMenuCommand;
import com.lawfirm.application.system.dto.MenuDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.Menu;
import com.lawfirm.domain.system.repository.MenuRepository;
import com.lawfirm.infrastructure.cache.BusinessCacheService;
import com.lawfirm.infrastructure.persistence.mapper.MenuMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 菜单应用服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuAppService {

  /** 菜单仓储 */
  private final MenuRepository menuRepository;

  /** 菜单Mapper */
  private final MenuMapper menuMapper;

  /** 业务缓存服务 */
  private final BusinessCacheService businessCacheService;

  /**
   * 获取菜单树
   *
   * @return 菜单树
   */
  public List<MenuDTO> getMenuTree() {
    List<Menu> allMenus = menuMapper.selectAllMenus();
    return buildTree(allMenus, 0L);
  }

  /**
   * 获取用户菜单树（带缓存） 只返回 MENU 和 DIRECTORY 类型，不返回 BUTTON 类型（按钮权限）
   *
   * @param userId 用户ID
   * @return 菜单树
   */
  public List<MenuDTO> getUserMenuTree(final Long userId) {
    return businessCacheService.getUserMenus(
        userId,
        () -> {
          List<Menu> userMenus = menuMapper.selectByUserId(userId);
          // 过滤掉 BUTTON 类型的菜单，只保留 MENU 和 DIRECTORY
          List<Menu> filteredMenus =
              userMenus.stream()
                  .filter(menu -> !"BUTTON".equals(menu.getMenuType()))
                  .collect(Collectors.toList());
          return buildTree(filteredMenus, 0L);
        });
  }

  /**
   * 获取角色菜单ID列表
   *
   * @param roleId 角色ID
   * @return 菜单ID列表
   */
  public List<Long> getRoleMenuIds(final Long roleId) {
    return menuMapper.selectByRoleId(roleId).stream().map(Menu::getId).collect(Collectors.toList());
  }

  /**
   * 分配角色菜单 优化：使用批量插入替代循环插入，提升性能 问题501修复：改进异常处理
   *
   * @param roleId 角色ID
   * @param menuIds 菜单ID列表
   */
  @Transactional
  public void assignRoleMenus(final Long roleId, final List<Long> menuIds) {
    try {
      // 验证角色是否存在
      if (roleId == null) {
        throw new BusinessException("角色ID不能为空");
      }

      // 先删除原有关联
      menuMapper.deleteRoleMenus(roleId);

      // 再批量插入新关联
      if (menuIds != null && !menuIds.isEmpty()) {
        // 去重并过滤空值
        List<Long> uniqueMenuIds =
            menuIds.stream().filter(id -> id != null).distinct().collect(Collectors.toList());

        if (!uniqueMenuIds.isEmpty()) {
          // 使用批量插入，性能更好
          menuMapper.batchInsertRoleMenus(roleId, uniqueMenuIds);
        }
      }
      log.info("角色菜单分配成功: roleId={}, menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);

      // 清除所有用户菜单缓存（因为角色菜单变更会影响多个用户）
      businessCacheService.evictAllMenus();
    } catch (DataIntegrityViolationException e) {
      // 问题501修复：只捕获预期异常
      log.error("分配角色菜单数据库约束冲突: roleId={}, menuIds={}", roleId, menuIds, e);
      throw new BusinessException("分配角色菜单失败：数据库约束冲突");
    } catch (BusinessException e) {
      // 问题501修复：重新抛出业务异常
      throw e;
    }
    // 其他异常不捕获，让Spring事务管理器处理
  }

  /**
   * 获取菜单详情
   *
   * @param id 菜单ID
   * @return 菜单DTO
   */
  public MenuDTO getMenuById(final Long id) {
    Menu menu = menuRepository.getByIdOrThrow(id, "菜单不存在");
    return toDTO(menu);
  }

  /**
   * 创建菜单
   *
   * @param command 创建命令
   * @return 菜单DTO
   */
  @Transactional
  public MenuDTO createMenu(final CreateMenuCommand command) {
    Menu menu =
        Menu.builder()
            .parentId(command.getParentId() != null ? command.getParentId() : 0L)
            .name(command.getName())
            .path(command.getPath())
            .component(command.getComponent())
            .redirect(command.getRedirect())
            .icon(command.getIcon())
            .menuType(command.getMenuType())
            .permission(command.getPermission())
            .sortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0)
            .visible(command.getVisible() != null ? command.getVisible() : true)
            .status(Menu.STATUS_ENABLED)
            .isExternal(command.getIsExternal() != null ? command.getIsExternal() : false)
            .isCache(command.getIsCache() != null ? command.getIsCache() : true)
            .build();

    menuRepository.save(menu);
    log.info("菜单创建成功: {}", menu.getName());

    // 清除所有菜单缓存
    businessCacheService.evictAllMenus();

    return toDTO(menu);
  }

  /**
   * 更新菜单 问题496修复：完善循环引用检查
   *
   * @param command 更新命令
   * @return 菜单DTO
   */
  @Transactional
  public MenuDTO updateMenu(final UpdateMenuCommand command) {
    Menu menu = menuRepository.getByIdOrThrow(command.getId(), "菜单不存在");

    if (command.getParentId() != null) {
      if (command.getParentId().equals(command.getId())) {
        throw new BusinessException("父菜单不能是自己");
      }

      // 问题496修复：检查是否形成循环
      if (willFormCycle(command.getId(), command.getParentId())) {
        throw new BusinessException("不能形成循环引用的菜单结构");
      }

      menu.setParentId(command.getParentId());
    }
    if (StringUtils.hasText(command.getName())) {
      menu.setName(command.getName());
    }
    if (command.getPath() != null) {
      menu.setPath(command.getPath());
    }
    if (command.getComponent() != null) {
      menu.setComponent(command.getComponent());
    }
    if (command.getRedirect() != null) {
      menu.setRedirect(command.getRedirect());
    }
    if (command.getIcon() != null) {
      menu.setIcon(command.getIcon());
    }
    if (command.getMenuType() != null) {
      menu.setMenuType(command.getMenuType());
    }
    if (command.getPermission() != null) {
      menu.setPermission(command.getPermission());
    }
    if (command.getSortOrder() != null) {
      menu.setSortOrder(command.getSortOrder());
    }
    if (command.getVisible() != null) {
      menu.setVisible(command.getVisible());
    }
    if (command.getStatus() != null) {
      menu.setStatus(command.getStatus());
    }
    if (command.getIsExternal() != null) {
      menu.setIsExternal(command.getIsExternal());
    }
    if (command.getIsCache() != null) {
      menu.setIsCache(command.getIsCache());
    }

    menuRepository.updateById(menu);
    log.info("菜单更新成功: {}", menu.getName());

    // 清除所有菜单缓存
    businessCacheService.evictAllMenus();

    return toDTO(menu);
  }

  /**
   * 删除菜单 问题495修复：检查角色关联
   *
   * @param id 菜单ID
   */
  @Transactional
  public void deleteMenu(final Long id) {
    Menu menu = menuRepository.getByIdOrThrow(id, "菜单不存在");

    // 检查是否有子菜单
    List<Menu> children = menuMapper.selectByParentId(id);
    if (!children.isEmpty()) {
      throw new BusinessException("存在子菜单，无法删除");
    }

    // 问题495修复：检查是否有角色关联
    long roleCount = menuMapper.countRoleMenus(id);
    if (roleCount > 0) {
      throw new BusinessException("该菜单已分配给" + roleCount + "个角色，无法删除");
    }

    menuMapper.deleteById(id);
    log.info("菜单删除成功: {}", menu.getName());

    // 清除所有菜单缓存
    businessCacheService.evictAllMenus();
  }

  /**
   * 检查是否形成循环引用
   *
   * @param menuId 菜单ID
   * @param newParentId 新父菜单ID
   * @return 是否形成循环
   */
  private boolean willFormCycle(final Long menuId, final Long newParentId) {
    if (newParentId == null || newParentId == 0L) {
      return false;
    }

    Long currentParentId = newParentId;
    Set<Long> visited = new HashSet<>();
    visited.add(menuId);

    while (currentParentId != null && currentParentId != 0L) {
      // 检查是否回到起点
      if (currentParentId.equals(menuId)) {
        return true; // 形成循环
      }

      // 检查是否已访问（检测循环）
      if (visited.contains(currentParentId)) {
        log.warn(
            "检测到菜单数据中存在循环引用: menuId={}, currentParentId={}, visited={}",
            menuId,
            currentParentId,
            visited);
        return true;
      }

      visited.add(currentParentId);

      // 获取下一个父节点
      Menu parent = menuRepository.getById(currentParentId);
      currentParentId = parent != null ? parent.getParentId() : null;
    }

    return false;
  }

  /** 菜单树最大深度限制 */
  private static final int MAX_MENU_DEPTH = 10;

  /**
   * 构建菜单树
   *
   * @param menus 菜单列表
   * @param parentId 父菜单ID
   * @return 菜单树
   */
  private List<MenuDTO> buildTree(final List<Menu> menus, final Long parentId) {
    Map<Long, List<Menu>> grouped =
        menus.stream().collect(Collectors.groupingBy(Menu::getParentId));

    return buildTreeRecursive(grouped, parentId, 0, new HashSet<>());
  }

  /**
   * 递归构建菜单树
   *
   * @param grouped 按父ID分组的菜单Map
   * @param parentId 当前父ID
   * @param depth 当前深度
   * @param visited 已访问的节点ID集合（用于循环检测）
   * @return 菜单树列表
   */
  private List<MenuDTO> buildTreeRecursive(
      final Map<Long, List<Menu>> grouped,
      final Long parentId,
      final int depth,
      final Set<Long> visited) {
    // 深度限制检查
    if (depth >= MAX_MENU_DEPTH) {
      log.warn("菜单树深度超过最大限制{}，停止递归，parentId={}", MAX_MENU_DEPTH, parentId);
      return Collections.emptyList();
    }

    // 循环引用检测
    if (parentId != null && parentId != 0L && visited.contains(parentId)) {
      log.warn("检测到菜单循环引用，parentId={}，已访问节点={}", parentId, visited);
      return Collections.emptyList();
    }

    // 将当前父ID加入已访问集合（排除根节点0）
    if (parentId != null && parentId != 0L) {
      visited.add(parentId);
    }

    List<Menu> children = grouped.get(parentId);
    if (children == null) {
      return new ArrayList<>();
    }

    return children.stream()
        .map(
            menu -> {
              MenuDTO dto = toDTO(menu);
              // 传递visited副本，防止兄弟节点之间互相影响
              dto.setChildren(
                  buildTreeRecursive(grouped, menu.getId(), depth + 1, new HashSet<>(visited)));
              return dto;
            })
        .collect(Collectors.toList());
  }

  /**
   * 转换为DTO
   *
   * @param menu 菜单实体
   * @return 菜单DTO
   */
  private MenuDTO toDTO(final Menu menu) {
    MenuDTO dto = new MenuDTO();
    dto.setId(menu.getId());
    dto.setParentId(menu.getParentId());
    dto.setName(menu.getName());
    dto.setPath(menu.getPath());
    dto.setComponent(menu.getComponent());
    dto.setRedirect(menu.getRedirect());
    dto.setIcon(menu.getIcon());
    dto.setMenuType(menu.getMenuType());
    dto.setMenuTypeName(getMenuTypeName(menu.getMenuType()));
    dto.setPermission(menu.getPermission());
    dto.setSortOrder(menu.getSortOrder());
    dto.setVisible(menu.getVisible());
    dto.setStatus(menu.getStatus());
    dto.setIsExternal(menu.getIsExternal());
    dto.setIsCache(menu.getIsCache());
    dto.setCreatedAt(menu.getCreatedAt());
    dto.setUpdatedAt(menu.getUpdatedAt());
    return dto;
  }

  /**
   * 获取菜单类型名称
   *
   * @param type 菜单类型
   * @return 类型名称
   */
  private String getMenuTypeName(final String type) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "DIRECTORY" -> "目录";
      case "MENU" -> "菜单";
      case "BUTTON" -> "按钮";
      default -> type;
    };
  }
}
