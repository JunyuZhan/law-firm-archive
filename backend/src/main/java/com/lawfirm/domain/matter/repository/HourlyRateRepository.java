package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.HourlyRate;
import com.lawfirm.infrastructure.persistence.mapper.HourlyRateMapper;
import java.time.LocalDate;
import org.springframework.stereotype.Repository;

/**
 * 小时费率仓储。
 *
 * <p>提供小时费率数据的持久化操作。
 */
@Repository
public class HourlyRateRepository extends AbstractRepository<HourlyRateMapper, HourlyRate> {

  /**
   * 获取用户当前有效费率。
   *
   * @param userId 用户ID
   * @param date 日期
   * @return 小时费率
   */
  public HourlyRate findCurrentRate(final Long userId, final LocalDate date) {
    return baseMapper.selectCurrentRate(userId, date);
  }

  /**
   * 获取用户最新费率。
   *
   * @param userId 用户ID
   * @return 小时费率
   */
  public HourlyRate findLatestRate(final Long userId) {
    return baseMapper.selectLatestRate(userId);
  }
}
