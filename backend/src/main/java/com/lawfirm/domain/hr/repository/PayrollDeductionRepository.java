package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollDeduction;
import com.lawfirm.infrastructure.persistence.mapper.PayrollDeductionMapper;
import java.util.List;
import org.springframework.stereotype.Repository;

/** 工资扣减项 Repository. */
@Repository
public class PayrollDeductionRepository
    extends AbstractRepository<PayrollDeductionMapper, PayrollDeduction> {

  /**
   * 根据工资明细ID查询所有扣减项.
   *
   * @param payrollItemId 工资明细ID
   * @return 扣减项列表
   */
  public List<PayrollDeduction> findByPayrollItemId(final Long payrollItemId) {
    return baseMapper.selectByPayrollItemId(payrollItemId);
  }

  /**
   * 批量根据工资明细ID查询所有扣减项（避免N+1查询）.
   *
   * @param payrollItemIds 工资明细ID列表
   * @return 扣减项列表
   */
  public List<PayrollDeduction> findByPayrollItemIds(final List<Long> payrollItemIds) {
    if (payrollItemIds == null || payrollItemIds.isEmpty()) {
      return java.util.Collections.emptyList();
    }
    return baseMapper.selectByPayrollItemIds(payrollItemIds);
  }

  /**
   * 批量查询并按工资明细ID分组（避免N+1查询）.
   *
   * @param payrollItemIds 工资明细ID列表
   * @return Map<工资明细ID, 扣减项列表>
   */
  public java.util.Map<Long, List<PayrollDeduction>> findByPayrollItemIdsGrouped(
      final List<Long> payrollItemIds) {
    List<PayrollDeduction> all = findByPayrollItemIds(payrollItemIds);
    return all.stream()
        .collect(java.util.stream.Collectors.groupingBy(PayrollDeduction::getPayrollItemId));
  }
}
