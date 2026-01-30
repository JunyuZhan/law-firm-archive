package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Deadline;
import com.lawfirm.infrastructure.persistence.mapper.DeadlineMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 期限提醒仓储。
 *
 * <p>提供期限提醒数据的持久化操作。
 */
@Repository
public class DeadlineRepository extends AbstractRepository<DeadlineMapper, Deadline> {

  /**
   * 查询需要提醒的期限。
   *
   * @return 需要提醒的期限列表
   */
  public List<Deadline> findNeedReminder() {
    return baseMapper.selectNeedReminder();
  }

  /**
   * 查询即将过期的期限。
   *
   * @return 即将过期的期限列表
   */
  public List<Deadline> findUpcomingDeadlines() {
    return baseMapper.selectUpcomingDeadlines();
  }

  /**
   * 根据项目ID查询期限列表。
   *
   * @param matterId 案件ID
   * @return 期限列表
   */
  public List<Deadline> findByMatterId(final Long matterId) {
    return baseMapper.selectByMatterId(matterId);
  }

  /**
   * 查询用户即将到期的期限。
   *
   * @param userId 用户ID
   * @param days 天数
   * @param limit 查询数量限制
   * @return 即将到期的期限列表
   */
  public List<Deadline> findMyUpcoming(final Long userId, final Integer days, final Integer limit) {
    return baseMapper.selectMyUpcoming(userId, days, limit);
  }
}
