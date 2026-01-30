package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PayrollIncome;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 工资收入项 Mapper */
@Mapper
public interface PayrollIncomeMapper extends BaseMapper<PayrollIncome> {

  /**
   * 根据工资明细ID查询所有收入项.
   *
   * @param payrollItemId 工资明细ID
   * @return 收入项列表
   */
  @Select(
      "SELECT * FROM hr_payroll_income WHERE payroll_item_id = #{payrollItemId} "
          + "AND deleted = false ORDER BY income_type")
  List<PayrollIncome> selectByPayrollItemId(@Param("payrollItemId") Long payrollItemId);
}
