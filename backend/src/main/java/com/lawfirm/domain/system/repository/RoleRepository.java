package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.Role;
import com.lawfirm.infrastructure.persistence.mapper.RoleMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 角色仓储。
 *
 * <p>提供角色数据的持久化操作。
 */
@Repository
public class RoleRepository extends AbstractRepository<RoleMapper, Role> {

  /**
   * 根据用户ID查询角色列表。
   *
   * @param userId 用户ID
   * @return 角色列表
   */
  public List<Role> findByUserId(final Long userId) {
    return baseMapper.selectByUserId(userId);
  }

  /**
   * 根据角色编码查询。
   *
   * @param roleCode 角色编码
   * @return 角色信息
   */
  public Role findByRoleCode(final String roleCode) {
    return baseMapper.selectByRoleCode(roleCode);
  }

  /**
   * 检查角色编码是否存在。
   *
   * @param roleCode 角色编码
   * @return 是否存在
   */
  public boolean existsByRoleCode(final String roleCode) {
    return baseMapper.selectByRoleCode(roleCode) != null;
  }
}
