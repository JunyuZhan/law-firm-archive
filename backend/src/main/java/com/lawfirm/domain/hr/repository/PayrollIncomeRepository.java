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
}
