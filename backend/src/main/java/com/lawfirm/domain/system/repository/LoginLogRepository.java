package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.LoginLog;
import com.lawfirm.infrastructure.persistence.mapper.LoginLogMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 登录日志仓储。
 *
 * <p>提供登录日志的持久化操作。
 */
@Repository
public class LoginLogRepository extends AbstractRepository<LoginLogMapper, LoginLog> {

  /**
   * 查询用户登录日志。
   *
   * @param userId 用户ID
   * @param offset 偏移量
   * @param limit 查询数量限制
   * @return 登录日志列表
   */
  public List<LoginLog> findByUserId(final Long userId, final int offset, final int limit) {
    return baseMapper.selectByUserId(userId, offset, limit);
  }

  /**
   * 查询指定时间范围内的登录日志。
   *
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param username 用户名
   * @param status 状态
   * @param offset 偏移量
   * @param limit 查询数量限制
   * @return 登录日志列表
   */
  public List<LoginLog> findByTimeRange(
      final LocalDateTime startTime,
      final LocalDateTime endTime,
      final String username,
      final String status,
      final int offset,
      final int limit) {
    return baseMapper.selectByTimeRange(startTime, endTime, username, status, offset, limit);
  }

  /**
   * 统计登录失败次数。
   *
   * @param username 用户名
   * @param startTime 开始时间
   * @return 失败次数
   */
  public int countFailureByUsername(final String username, final LocalDateTime startTime) {
    return baseMapper.countFailureByUsername(username, startTime);
  }
}
