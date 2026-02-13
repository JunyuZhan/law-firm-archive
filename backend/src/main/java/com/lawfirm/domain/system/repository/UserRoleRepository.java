package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.UserRole;
import com.lawfirm.infrastructure.persistence.mapper.UserRoleMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 用户角色关联仓储。
 *
 * <p>提供用户角色关联数据的持久化操作。
 */
@Repository
public class UserRoleRepository extends ServiceImpl<UserRoleMapper, UserRole> {

  /**
   * 根据用户ID删除关联。
   *
   * @param userId 用户ID
   */
  public void deleteByUserId(final Long userId) {
    baseMapper.deleteByUserId(userId);
  }

  /**
   * 根据角色ID删除关联。
   *
   * @param roleId 角色ID
   */
  public void deleteByRoleId(final Long roleId) {
    baseMapper.deleteByRoleId(roleId);
  }

  /**
   * 根据用户ID查询角色ID列表。
   *
   * @param userId 用户ID
   * @return 角色ID列表
   */
  public List<Long> findRoleIdsByUserId(final Long userId) {
    return baseMapper.selectRoleIdsByUserId(userId);
  }

  /**
   * 根据角色ID查询用户ID列表。
   *
   * @param roleId 角色ID
   * @return 用户ID列表
   */
  public List<Long> findUserIdsByRoleId(final Long roleId) {
    return baseMapper.selectUserIdsByRoleId(roleId);
  }
}
