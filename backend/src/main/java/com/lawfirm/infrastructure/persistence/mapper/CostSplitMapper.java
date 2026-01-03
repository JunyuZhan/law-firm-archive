package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.CostSplit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 成本分摊 Mapper
 */
@Mapper
public interface CostSplitMapper extends BaseMapper<CostSplit> {

    /**
     * 根据费用ID查询所有分摊记录
     */
    @Select("SELECT * FROM finance_cost_split WHERE expense_id = #{expenseId} AND deleted = false")
    List<CostSplit> selectByExpenseId(@Param("expenseId") Long expenseId);

    /**
     * 根据项目ID查询所有分摊记录
     */
    @Select("SELECT * FROM finance_cost_split WHERE matter_id = #{matterId} AND deleted = false")
    List<CostSplit> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 统计项目的分摊成本总额
     */
    @Select("""
        SELECT COALESCE(SUM(split_amount), 0) 
        FROM finance_cost_split 
        WHERE matter_id = #{matterId} AND deleted = false
        """)
    java.math.BigDecimal selectTotalSplitCostByMatterId(@Param("matterId") Long matterId);
}

