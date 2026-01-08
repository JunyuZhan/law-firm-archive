package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PayrollDeduction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工资扣减项 Mapper
 */
@Mapper
public interface PayrollDeductionMapper extends BaseMapper<PayrollDeduction> {

    /**
     * 根据工资明细ID查询所有扣减项
     */
    @Select("SELECT * FROM hr_payroll_deduction WHERE payroll_item_id = #{payrollItemId} AND deleted = false ORDER BY deduction_type")
    List<PayrollDeduction> selectByPayrollItemId(@Param("payrollItemId") Long payrollItemId);
}

