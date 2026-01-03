package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Role;
import com.lawfirm.infrastructure.persistence.mapper.RoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 角色仓储
 */
@Repository
public class RoleRepository extends AbstractRepository<RoleMapper, Role> {

    /**
     * 根据用户ID查询角色列表
     */
    public List<Role> findByUserId(Long userId) {
        return baseMapper.selectByUserId(userId);
    }

    /**
     * 根据角色编码查询
     */
    public Role findByRoleCode(String roleCode) {
        return baseMapper.selectByRoleCode(roleCode);
    }

    /**
     * 检查角色编码是否存在
     */
    public boolean existsByRoleCode(String roleCode) {
        return baseMapper.selectByRoleCode(roleCode) != null;
    }
}
