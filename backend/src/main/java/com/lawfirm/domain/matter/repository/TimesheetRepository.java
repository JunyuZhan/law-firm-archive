package com.lawfirm.domain.matter.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.matter.entity.Timesheet;
import com.lawfirm.infrastructure.persistence.mapper.TimesheetMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 工时记录仓储。
 *
 * <p>提供工时记录数据的持久化操作。
 */
@Repository
public class TimesheetRepository extends AbstractRepository<TimesheetMapper, Timesheet> {

  /**
   * 按用户和日期范围查询。
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 工时记录列表
   */
  public List<Timesheet> findByUserAndDateRange(
      final Long userId, final LocalDate startDate, final LocalDate endDate) {
    return baseMapper.selectByUserAndDateRange(userId, startDate, endDate);
  }

  /**
   * 统计用户某月总工时。
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 总工时
   */
  public BigDecimal sumHoursByUserAndMonth(final Long userId, final int year, final int month) {
    return baseMapper.sumHoursByUserAndMonth(userId, year, month);
  }

  /**
   * 统计案件总工时。
   *
   * @param matterId 案件ID
   * @return 总工时
   */
  public BigDecimal sumHoursByMatter(final Long matterId) {
    return baseMapper.sumHoursByMatter(matterId);
  }

  /**
   * 查询待审批工时。
   *
   * @return 待审批工时列表
   */
  public List<Timesheet> findPendingApproval() {
    return baseMapper.selectPendingApproval();
  }
}
