package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储
 */
@Repository
public class UserRepository extends AbstractRepository<UserMapper, User> {

    /**
     * 根据用户名查询
     */
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(baseMapper.selectByUsername(username));
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * 获取用户的角色编码列表
     */
    public List<String> findRoleCodesByUserId(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * 获取用户的权限编码列表
     */
    public List<String> findPermissionsByUserId(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
    }
}
