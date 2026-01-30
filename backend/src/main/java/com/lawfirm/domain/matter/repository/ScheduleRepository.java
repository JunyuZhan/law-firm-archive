package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Schedule;
import com.lawfirm.infrastructure.persistence.mapper.ScheduleMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 日程仓储。
 *
 * <p>提供日程数据的持久化操作。
 */
@Repository
public class ScheduleRepository extends AbstractRepository<ScheduleMapper, Schedule> {

  /**
   * 查询用户某天的日程。
   *
   * @param userId 用户ID
   * @param date 日期
   * @return 日程列表
   */
  public List<Schedule> findByUserAndDate(final Long userId, final LocalDate date) {
    return baseMapper.selectByUserAndDate(userId, date);
  }

  /**
   * 查询需要提醒的日程。
   *
   * @return 需要提醒的日程列表
   */
  public List<Schedule> findNeedReminder() {
    return baseMapper.selectNeedReminder();
  }
}
