package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PayrollDeduction;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 工资扣减项 Mapper */
@Mapper
public interface PayrollDeductionMapper extends BaseMapper<PayrollDeduction> {

  /**
   * 根据工资明细ID查询所有扣减项.
   *
   * @param payrollItemId 工资明细ID
   * @return 扣减项列表
   */
  @Select(
      "SELECT * FROM hr_payroll_deduction WHERE payroll_item_id = #{payrollItemId} "
          + "AND deleted = false ORDER BY deduction_type")
  List<PayrollDeduction> selectByPayrollItemId(@Param("payrollItemId") Long payrollItemId);

  /**
   * 批量根据工资明细ID查询所有扣减项（避免N+1查询）.
   *
   * @param payrollItemIds 工资明细ID列表
   * @return 扣减项列表
   */
  @Select(
      "<script>"
          + "SELECT * FROM hr_payroll_deduction WHERE payroll_item_id IN "
          + "<foreach collection='payrollItemIds' item='id' open='(' separator=',' close=')'>"
          + "#{id}"
          + "</foreach>"
          + " AND deleted = false ORDER BY payroll_item_id, deduction_type"
          + "</script>")
  List<PayrollDeduction> selectByPayrollItemIds(@Param("payrollItemIds") List<Long> payrollItemIds);
}
