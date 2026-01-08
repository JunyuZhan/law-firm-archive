package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollDeduction;
import com.lawfirm.infrastructure.persistence.mapper.PayrollDeductionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 工资扣减项 Repository
 */
@Repository
public class PayrollDeductionRepository extends AbstractRepository<PayrollDeductionMapper, PayrollDeduction> {

    /**
     * 根据工资明细ID查询所有扣减项
     */
    public List<PayrollDeduction> findByPayrollItemId(Long payrollItemId) {
        return baseMapper.selectByPayrollItemId(payrollItemId);
    }
}

