package com.archivesystem.service.impl;

import com.archivesystem.common.exception.BusinessException;
import com.archivesystem.common.exception.NotFoundException;
import com.archivesystem.entity.Role;
import com.archivesystem.entity.UserRole;
import com.archivesystem.repository.RoleMapper;
import com.archivesystem.repository.UserRoleMapper;
import com.archivesystem.service.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.List;

/**
 * 角色服务实现.
 * @author junyuzhan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private static final Set<String> BUILT_IN_ROLE_CODES = Set.of(
            "SYSTEM_ADMIN",
            "SECURITY_ADMIN",
            "AUDIT_ADMIN",
            "ARCHIVE_MANAGER",
            "ARCHIVE_REVIEWER",
            "USER"
    );

    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional
    public Role create(Role role) {
        // 检查角色代码是否存在
        if (roleMapper.selectByRoleCode(role.getRoleCode()) != null) {
            throw new BusinessException("角色代码已存在: " + role.getRoleCode());
        }

        role.setStatus(Role.STATUS_ACTIVE);
        roleMapper.insert(role);
        log.info("创建角色: id={}, code={}", role.getId(), role.getRoleCode());

        return role;
    }

    @Override
    @Transactional
    public Role update(Long id, Role role) {
        Role existing = roleMapper.selectById(id);
        if (existing == null) {
            throw NotFoundException.of("角色", id);
        }

        boolean builtInRole = isBuiltInRole(existing);
        if (builtInRole && Role.STATUS_DISABLED.equals(role.getStatus())) {
            throw new BusinessException("系统内置角色不可停用");
        }

        // 如果修改了角色代码，检查是否重复
        if (!existing.getRoleCode().equals(role.getRoleCode())) {
            if (builtInRole) {
                throw new BusinessException("系统内置角色代码不可修改");
            }
            Role byCode = roleMapper.selectByRoleCode(role.getRoleCode());
            if (byCode != null && !byCode.getId().equals(id)) {
                throw new BusinessException("角色代码已存在: " + role.getRoleCode());
            }
            existing.setRoleCode(role.getRoleCode());
        }

        existing.setRoleName(role.getRoleName());
        existing.setDescription(role.getDescription());
        existing.setStatus(role.getStatus());

        roleMapper.updateById(existing);
        return existing;
    }

    @Override
    public Role getById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw NotFoundException.of("角色", id);
        }
        return role;
    }

    @Override
    public List<Role> list() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getDeleted, false)
               .orderByAsc(Role::getId);
        return roleMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            return;
        }

        if (isBuiltInRole(role)) {
            throw new BusinessException("系统内置角色不可删除");
        }

        // 检查是否有用户使用此角色
        long count = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, id));
        if (count > 0) {
            throw new BusinessException("该角色下存在 " + count + " 个用户，无法删除");
        }

        roleMapper.deleteById(id);
        log.info("删除角色: id={}", id);
    }

    private boolean isBuiltInRole(Role role) {
        return role != null && BUILT_IN_ROLE_CODES.contains(role.getRoleCode());
    }
}
