package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollItem;
import com.lawfirm.infrastructure.persistence.mapper.PayrollItemMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 工资明细 Repository
 */
@Repository
public class PayrollItemRepository extends AbstractRepository<PayrollItemMapper, PayrollItem> {

    /**
     * 根据工资表ID查询所有工资明细
     */
    public List<PayrollItem> findByPayrollSheetId(Long payrollSheetId) {
        return baseMapper.selectByPayrollSheetId(payrollSheetId);
    }

    /**
     * 根据员工ID和年月查询工资明细
     */
    public Optional<PayrollItem> findByEmployeeIdAndYearMonth(Long employeeId, Integer year, Integer month) {
        PayrollItem item = baseMapper.selectByEmployeeIdAndYearMonth(employeeId, year, month);
        return Optional.ofNullable(item);
    }

    /**
     * 统计工资表中已确认的人数
     */
    public Integer countConfirmedByPayrollSheetId(Long payrollSheetId) {
        Integer count = baseMapper.countConfirmedByPayrollSheetId(payrollSheetId);
        return count != null ? count : 0;
    }
}

