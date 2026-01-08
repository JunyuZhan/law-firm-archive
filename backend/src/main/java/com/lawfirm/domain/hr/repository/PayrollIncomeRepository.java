package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollIncome;
import com.lawfirm.infrastructure.persistence.mapper.PayrollIncomeMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工资收入项 Repository
 */
@Repository
public class PayrollIncomeRepository extends AbstractRepository<PayrollIncomeMapper, PayrollIncome> {

    /**
     * 根据工资明细ID查询所有收入项
     */
    public List<PayrollIncome> findByPayrollItemId(Long payrollItemId) {
        return baseMapper.selectByPayrollItemId(payrollItemId);
    }
}

