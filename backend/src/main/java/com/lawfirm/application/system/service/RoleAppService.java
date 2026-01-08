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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleAppService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMenuMapper roleMenuMapper;
    private final MenuMapper menuMapper;
    private final PermissionChangeLogRepository permissionChangeLogRepository;

    /**
     * 分页查询角色列表
     */
    public PageResult<RoleDTO> listRoles(PageQuery query, String keyword) {
        Page<Role> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Role::getRoleName, keyword)
                   .or()
                   .like(Role::getRoleCode, keyword);
        }
        wrapper.orderByAsc(Role::getSortOrder);
        
        Page<Role> result = roleRepository.page(page, wrapper);
        
        List<RoleDTO> items = result.getRecords().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return PageResult.of(items, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 获取所有角色（下拉选择用）
     */
    public List<RoleDTO> getAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, "ACTIVE")
               .orderByAsc(Role::getSortOrder);
        
        return roleRepository.list(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取角色详情
     */
    public RoleDTO getRoleById(Long id) {
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
     */
    @Transactional
    public RoleDTO createRole(CreateRoleCommand command) {
        // 检查角色编码是否存在
        if (roleRepository.existsByRoleCode(command.getRoleCode())) {
            throw new BusinessException("角色编码已存在");
        }
        
        Role role = Role.builder()
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
     */
    @Transactional
    public RoleDTO updateRole(UpdateRoleCommand command) {
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
     */
    @Transactional
    public void deleteRole(Long id) {
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
     */
    @Transactional
    public void changeStatus(Long id, String status) {
        Role role = roleRepository.getById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        
        role.setStatus(status);
        roleRepository.updateById(role);
        
        log.info("修改角色状态: {} -> {}", role.getRoleCode(), status);
    }

    /**
     * 分配角色菜单
     */
    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
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
        
        // 删除原有关联
        roleMenuMapper.deleteByRoleId(roleId);
        
        // 保存新关联
        if (menuIds != null && !menuIds.isEmpty()) {
            saveRoleMenus(roleId, menuIds);
        }
        
        // 记录权限变更历史
        Long changedBy = SecurityUtils.getUserId();
        LocalDateTime changedAt = LocalDateTime.now();
        
        // 记录新增的权限
        for (Long menuId : addedMenuIds) {
            Menu menu = menuMapper.selectById(menuId);
            if (menu != null && menu.getPermission() != null && !menu.getPermission().isEmpty()) {
                PermissionChangeLog changeLog = PermissionChangeLog.builder()
                        .roleId(roleId)
                        .roleCode(role.getRoleCode())
                        .changeType("ADD")
                        .permissionCode(menu.getPermission())
                        .permissionName(menu.getName())
                        .changedBy(changedBy)
                        .changedAt(changedAt)
                        .build();
                permissionChangeLogRepository.save(changeLog);
            }
        }
        
        // 记录移除的权限
        for (Long menuId : removedMenuIds) {
            Menu menu = menuMapper.selectById(menuId);
            if (menu != null && menu.getPermission() != null && !menu.getPermission().isEmpty()) {
                PermissionChangeLog changeLog = PermissionChangeLog.builder()
                        .roleId(roleId)
                        .roleCode(role.getRoleCode())
                        .changeType("REMOVE")
                        .permissionCode(menu.getPermission())
                        .permissionName(menu.getName())
                        .changedBy(changedBy)
                        .changedAt(changedAt)
                        .build();
                permissionChangeLogRepository.save(changeLog);
            }
        }
        
        log.info("分配角色菜单成功: roleId={}, menuCount={}, added={}, removed={}", 
                roleId, menuIds != null ? menuIds.size() : 0, addedMenuIds.size(), removedMenuIds.size());
    }

    /**
     * 获取角色的菜单ID列表
     */
    public List<Long> getRoleMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    private void saveRoleMenus(Long roleId, List<Long> menuIds) {
        List<RoleMenu> roleMenus = menuIds.stream()
                .map(menuId -> RoleMenu.builder()
                        .roleId(roleId)
                        .menuId(menuId)
                        .build())
                .collect(Collectors.toList());
        
        for (RoleMenu rm : roleMenus) {
            roleMenuMapper.insert(rm);
        }
    }

    private RoleDTO toDTO(Role role) {
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
