package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.PermissionChangeLog;
import com.lawfirm.infrastructure.persistence.mapper.PermissionChangeLogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 权限变更历史仓储。
 *
 * <p>提供权限变更历史的持久化操作。
 */
@Repository
public class PermissionChangeLogRepository
    extends ServiceImpl<PermissionChangeLogMapper, PermissionChangeLog> {

  /**
   * 根据角色ID查询权限变更历史。
   *
   * @param roleId 角色ID
   * @return 权限变更历史列表
   */
  public List<PermissionChangeLog> findByRoleId(final Long roleId) {
    return baseMapper.selectByRoleId(roleId);
  }

  /**
   * 根据变更类型查询权限变更历史。
   *
   * @param changeType 变更类型
   * @return 权限变更历史列表
   */
  public List<PermissionChangeLog> findByChangeType(final String changeType) {
    return baseMapper.selectByChangeType(changeType);
  }

  /**
   * 根据权限代码查询权限变更历史。
   *
   * @param permissionCode 权限代码
   * @return 权限变更历史列表
   */
  public List<PermissionChangeLog> findByPermissionCode(final String permissionCode) {
    return baseMapper.selectByPermissionCode(permissionCode);
  }
}
