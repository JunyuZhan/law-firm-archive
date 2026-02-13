package com.lawfirm.domain.system.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.system.entity.UserSession;
import com.lawfirm.infrastructure.persistence.mapper.UserSessionMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 用户会话仓储。
 *
 * <p>提供用户会话数据的持久化操作。
 */
@Repository
public class UserSessionRepository extends AbstractRepository<UserSessionMapper, UserSession> {

  /**
   * 根据Token查询会话。
   *
   * @param token 会话令牌
   * @return 用户会话
   */
  public Optional<UserSession> findByToken(final String token) {
    return Optional.ofNullable(baseMapper.selectByToken(token));
  }

  /**
   * 根据用户ID查询活跃会话。
   *
   * @param userId 用户ID
   * @return 活跃会话列表
   */
  public List<UserSession> findActiveSessionsByUserId(final Long userId) {
    return baseMapper.selectActiveSessionsByUserId(userId);
  }

  /**
   * 更新最后访问时间。
   *
   * @param id 会话ID
   * @param lastAccessTime 最后访问时间
   */
  public void updateLastAccessTime(final Long id, final java.time.LocalDateTime lastAccessTime) {
    baseMapper.updateLastAccessTime(id, lastAccessTime);
  }

  /**
   * 批量更新过期会话状态。
   *
   * @param now 当前时间
   * @return 更新数量
   */
  public int updateExpiredSessions(final java.time.LocalDateTime now) {
    return baseMapper.updateExpiredSessions(now);
  }

  /**
   * 将用户的其他会话标记为非当前会话。
   *
   * @param userId 用户ID
   * @param excludeId 排除的会话ID
   */
  public void markOtherSessionsAsNotCurrent(final Long userId, final Long excludeId) {
    baseMapper.markOtherSessionsAsNotCurrent(userId, excludeId);
  }

  /**
   * 根据Token更新最后访问时间（避免先查询再更新）。
   *
   * @param token 会话令牌
   * @param lastAccessTime 最后访问时间
   * @return 更新数量
   */
  public int updateLastAccessTimeByToken(
      final String token, final java.time.LocalDateTime lastAccessTime) {
    return baseMapper.updateLastAccessTimeByToken(token, lastAccessTime);
  }
}
