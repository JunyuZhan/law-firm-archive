package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.UserRole;
import com.lawfirm.infrastructure.persistence.mapper.UserRoleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色关联仓储
 */
@Repository
public class UserRoleRepository extends ServiceImpl<UserRoleMapper, UserRole> {

    /**
     * 根据用户ID删除关联
     */
    public void deleteByUserId(Long userId) {
        baseMapper.deleteByUserId(userId);
    }

    /**
     * 根据角色ID删除关联
     */
    public void deleteByRoleId(Long roleId) {
        baseMapper.deleteByRoleId(roleId);
    }

    /**
     * 根据用户ID查询角色ID列表
     */
    public List<Long> findRoleIdsByUserId(Long userId) {
        return baseMapper.selectRoleIdsByUserId(userId);
    }

    /**
     * 根据角色ID查询用户ID列表
     */
    public List<Long> findUserIdsByRoleId(Long roleId) {
        return baseMapper.selectUserIdsByRoleId(roleId);
    }
}
