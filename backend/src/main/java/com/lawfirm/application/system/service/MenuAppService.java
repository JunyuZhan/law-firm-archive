package com.lawfirm.application.system.service;

import com.lawfirm.application.system.command.CreateMenuCommand;
import com.lawfirm.application.system.command.UpdateMenuCommand;
import com.lawfirm.application.system.dto.MenuDTO;
import com.lawfirm.common.exception.BusinessException;
import com.lawfirm.domain.system.entity.Menu;
import com.lawfirm.domain.system.repository.MenuRepository;
import com.lawfirm.infrastructure.persistence.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuAppService {

    private final MenuRepository menuRepository;
    private final MenuMapper menuMapper;

    /**
     * 获取菜单树
     */
    public List<MenuDTO> getMenuTree() {
        List<Menu> allMenus = menuMapper.selectAllMenus();
        return buildTree(allMenus, 0L);
    }

    /**
     * 获取用户菜单树
     */
    public List<MenuDTO> getUserMenuTree(Long userId) {
        List<Menu> userMenus = menuMapper.selectByUserId(userId);
        return buildTree(userMenus, 0L);
    }

    /**
     * 获取角色菜单ID列表
     */
    public List<Long> getRoleMenuIds(Long roleId) {
        return menuMapper.selectByRoleId(roleId).stream()
                .map(Menu::getId)
                .collect(Collectors.toList());
    }

    /**
     * 分配角色菜单
     */
    @Transactional
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        // 先删除原有关联
        menuMapper.deleteRoleMenus(roleId);
        // 再插入新关联
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                menuMapper.insertRoleMenu(roleId, menuId);
            }
        }
        log.info("角色菜单分配成功: roleId={}, menuCount={}", roleId, menuIds != null ? menuIds.size() : 0);
    }

    /**
     * 获取菜单详情
     */
    public MenuDTO getMenuById(Long id) {
        Menu menu = menuRepository.getByIdOrThrow(id, "菜单不存在");
        return toDTO(menu);
    }

    /**
     * 创建菜单
     */
    @Transactional
    public MenuDTO createMenu(CreateMenuCommand command) {
        Menu menu = Menu.builder()
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
        return toDTO(menu);
    }

    /**
     * 更新菜单
     */
    @Transactional
    public MenuDTO updateMenu(UpdateMenuCommand command) {
        Menu menu = menuRepository.getByIdOrThrow(command.getId(), "菜单不存在");
        
        if (command.getParentId() != null) {
            if (command.getParentId().equals(command.getId())) {
                throw new BusinessException("父菜单不能是自己");
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
        return toDTO(menu);
    }

    /**
     * 删除菜单
     */
    @Transactional
    public void deleteMenu(Long id) {
        Menu menu = menuRepository.getByIdOrThrow(id, "菜单不存在");
        
        // 检查是否有子菜单
        List<Menu> children = menuMapper.selectByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("存在子菜单，无法删除");
        }
        
        menuMapper.deleteById(id);
        log.info("菜单删除成功: {}", menu.getName());
    }

    /**
     * 构建菜单树
     */
    private List<MenuDTO> buildTree(List<Menu> menus, Long parentId) {
        Map<Long, List<Menu>> grouped = menus.stream()
                .collect(Collectors.groupingBy(Menu::getParentId));
        
        return buildTreeRecursive(grouped, parentId);
    }

    private List<MenuDTO> buildTreeRecursive(Map<Long, List<Menu>> grouped, Long parentId) {
        List<Menu> children = grouped.get(parentId);
        if (children == null) {
            return new ArrayList<>();
        }
        
        return children.stream()
                .map(menu -> {
                    MenuDTO dto = toDTO(menu);
                    dto.setChildren(buildTreeRecursive(grouped, menu.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private MenuDTO toDTO(Menu menu) {
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

    private String getMenuTypeName(String type) {
        if (type == null) return null;
        return switch (type) {
            case "DIRECTORY" -> "目录";
            case "MENU" -> "菜单";
            case "BUTTON" -> "按钮";
            default -> type;
        };
    }
}
