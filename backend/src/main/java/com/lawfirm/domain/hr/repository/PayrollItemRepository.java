package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollItem;
import com.lawfirm.infrastructure.persistence.mapper.PayrollItemMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 工资明细 Repository. */
@Repository
public class PayrollItemRepository extends AbstractRepository<PayrollItemMapper, PayrollItem> {

  /**
   * 根据工资表ID查询所有工资明细.
   *
   * @param payrollSheetId 工资表ID
   * @return 工资明细列表
   */
  public List<PayrollItem> findByPayrollSheetId(final Long payrollSheetId) {
    return baseMapper.selectByPayrollSheetId(payrollSheetId);
  }

  /**
   * 根据员工ID和年月查询工资明细.
   *
   * @param employeeId 员工ID
   * @param year 年份
   * @param month 月份
   * @return 工资明细
   */
  public Optional<PayrollItem> findByEmployeeIdAndYearMonth(
      final Long employeeId, final Integer year, final Integer month) {
    PayrollItem item = baseMapper.selectByEmployeeIdAndYearMonth(employeeId, year, month);
    return Optional.ofNullable(item);
  }

  /**
   * 统计工资表中已确认的人数.
   *
   * @param payrollSheetId 工资表ID
   * @return 已确认人数
   */
  public Integer countConfirmedByPayrollSheetId(final Long payrollSheetId) {
    Integer count = baseMapper.countConfirmedByPayrollSheetId(payrollSheetId);
    return count != null ? count : 0;
  }
}
