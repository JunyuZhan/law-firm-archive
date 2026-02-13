package com.lawfirm.domain.hr.repository;

import com.lawfirm.common.base.AbstractRepository;
import com.lawfirm.domain.hr.entity.PayrollSheet;
import com.lawfirm.infrastructure.persistence.mapper.PayrollSheetMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 工资表 Repository. */
@Repository
public class PayrollSheetRepository extends AbstractRepository<PayrollSheetMapper, PayrollSheet> {

  /**
   * 根据工资表编号查询.
   *
   * @param payrollNo 工资表编号
   * @return 工资表
   */
  public Optional<PayrollSheet> findByPayrollNo(final String payrollNo) {
    PayrollSheet sheet = lambdaQuery().eq(PayrollSheet::getPayrollNo, payrollNo).one();
    return Optional.ofNullable(sheet);
  }

  /**
   * 根据年月查询工资表.
   *
   * @param year 年份
   * @param month 月份
   * @return 工资表
   */
  public Optional<PayrollSheet> findByYearAndMonth(final Integer year, final Integer month) {
    PayrollSheet sheet = baseMapper.selectByYearAndMonth(year, month);
    return Optional.ofNullable(sheet);
  }
}
