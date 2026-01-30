package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.CostAllocation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 成本归集 Mapper */
@Mapper
public interface CostAllocationMapper extends BaseMapper<CostAllocation> {

  /**
   * 查询项目的所有成本归集记录.
   *
   * @param matterId 项目ID
   * @return 成本归集记录列表
   */
  @Select(
      """
        SELECT ca.*, e.expense_no, e.description, e.expense_type, e.expense_date
        FROM finance_cost_allocation ca
        INNER JOIN finance_expense e ON ca.expense_id = e.id
        WHERE ca.matter_id = #{matterId}
        ORDER BY ca.allocation_date DESC, ca.created_at DESC
        """)
  List<CostAllocation> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 查询费用的归集记录.
   *
   * @param expenseId 费用ID
   * @return 成本归集记录列表
   */
  @Select(
      "SELECT * FROM finance_cost_allocation WHERE expense_id = #{expenseId} ORDER BY created_at DESC")
  List<CostAllocation> selectByExpenseId(@Param("expenseId") Long expenseId);
}
