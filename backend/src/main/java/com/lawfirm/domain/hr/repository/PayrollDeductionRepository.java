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
}
