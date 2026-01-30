package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.User;
import com.lawfirm.infrastructure.persistence.mapper.UserMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 用户仓储。
 *
 * <p>提供用户数据的持久化操作。
 */
@Repository
public class UserRepository extends AbstractRepository<UserMapper, User> {

  /**
   * 根据用户名查询。
   *
   * @param username 用户名
   * @return 用户信息
   */
  public Optional<User> findByUsername(final String username) {
    return Optional.ofNullable(baseMapper.selectByUsername(username));
  }

  /**
   * 检查用户名是否存在。
   *
   * @param username 用户名
   * @return 是否存在
   */
  public boolean existsByUsername(final String username) {
    return findByUsername(username).isPresent();
  }

  /**
   * 获取用户的角色编码列表。
   *
   * @param userId 用户ID
   * @return 角色编码列表
   */
  public List<String> findRoleCodesByUserId(final Long userId) {
    return baseMapper.selectRoleCodesByUserId(userId);
  }

  /**
   * 获取用户的权限编码列表。
   *
   * @param userId 用户ID
   * @return 权限编码列表
   */
  public List<String> findPermissionsByUserId(final Long userId) {
    return baseMapper.selectPermissionsByUserId(userId);
  }

  /**
   * 获取用户最高数据范围权限。
   *
   * <p>优先级: ALL > DEPT_AND_CHILD > DEPT > SELF
   *
   * @param userId 用户ID
   * @return 数据范围权限
   */
  public String findHighestDataScopeByUserId(final Long userId) {
    return baseMapper.selectHighestDataScopeByUserId(userId);
  }
}
