package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.TimerSession;
import com.lawfirm.infrastructure.persistence.mapper.TimerSessionMapper;
import org.springframework.stereotype.Repository;

/**
 * 计时器会话仓储。
 *
 * <p>提供计时器会话数据的持久化操作。
 */
@Repository
public class TimerSessionRepository extends AbstractRepository<TimerSessionMapper, TimerSession> {

  /**
   * 查询用户当前正在运行的计时器会话。
   *
   * @param userId 用户ID
   * @return 计时器会话
   */
  public TimerSession findRunningByUserId(final Long userId) {
    return baseMapper.selectRunningByUserId(userId);
  }

  /**
   * 查询用户当前已暂停的计时器会话。
   *
   * @param userId 用户ID
   * @return 计时器会话
   */
  public TimerSession findPausedByUserId(final Long userId) {
    return baseMapper.selectPausedByUserId(userId);
  }
}
