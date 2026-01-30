package com.lawfirm.domain.system.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lawfirm.domain.system.entity.RoleChangeLog;
import com.lawfirm.infrastructure.persistence.mapper.RoleChangeLogMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 角色变更历史仓储。
 *
 * <p>提供角色变更历史的持久化操作。
 */
@Repository
public class RoleChangeLogRepository extends ServiceImpl<RoleChangeLogMapper, RoleChangeLog> {

  /**
   * 根据用户ID查询角色变更历史。
   *
   * @param userId 用户ID
   * @return 角色变更历史列表
   */
  public List<RoleChangeLog> findByUserId(final Long userId) {
    return baseMapper.selectByUserId(userId);
  }

  /**
   * 根据变更类型查询角色变更历史。
   *
   * @param changeType 变更类型
   * @return 角色变更历史列表
   */
  public List<RoleChangeLog> findByChangeType(final String changeType) {
    return baseMapper.selectByChangeType(changeType);
  }
}
