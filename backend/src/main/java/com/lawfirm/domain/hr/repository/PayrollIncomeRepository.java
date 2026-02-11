package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollIncome;
import com.lawfirm.infrastructure.persistence.mapper.PayrollIncomeMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 工资收入项 Repository. */
@Repository
public class PayrollIncomeRepository
    extends AbstractRepository<PayrollIncomeMapper, PayrollIncome> {

  /**
   * 根据工资明细ID查询所有收入项.
   *
   * @param payrollItemId 工资明细ID
   * @return 收入项列表
   */
  public List<PayrollIncome> findByPayrollItemId(final Long payrollItemId) {
    return baseMapper.selectByPayrollItemId(payrollItemId);
  }

  /**
   * 批量根据工资明细ID查询所有收入项（避免N+1查询）.
   *
   * @param payrollItemIds 工资明细ID列表
   * @return 收入项列表
   */
  public List<PayrollIncome> findByPayrollItemIds(final List<Long> payrollItemIds) {
    if (payrollItemIds == null || payrollItemIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    return baseMapper.selectByPayrollItemIds(payrollItemIds);
  }

  /**
   * 批量查询并按工资明细ID分组（避免N+1查询）.
   *
   * @param payrollItemIds 工资明细ID列表
   * @return Map<工资明细ID, 收入项列表>
   */
  public java.util.Map<Long, List<PayrollIncome>> findByPayrollItemIdsGrouped(
      final List<Long> payrollItemIds) {
    List<PayrollIncome> all = findByPayrollItemIds(payrollItemIds);
    return all.stream()
        .collect(java.util.stream.Collectors.groupingBy(PayrollIncome::getPayrollItemId));
  }
}
