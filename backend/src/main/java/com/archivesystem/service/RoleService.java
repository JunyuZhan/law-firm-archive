package com.archivesystem.service;

import com.archivesystem.entity.Role;

import java.util.List;

/**
 * 角色服务接口.
 * @author junyuzhan
 */
public interface RoleService {

    /**
     * 创建角色.
     */
    Role create(Role role);

    /**
     * 更新角色.
     */
    Role update(Long id, Role role);

    /**
     * 获取角色详情.
     */
    Role getById(Long id);

    /**
     * 获取所有角色.
     */
    List<Role> list();

    /**
     * 删除角色.
     */
    void delete(Long id);
}
