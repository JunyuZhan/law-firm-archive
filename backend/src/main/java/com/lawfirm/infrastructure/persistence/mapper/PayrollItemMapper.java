package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.hr.entity.PayrollItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 工资明细 Mapper
 */
@Mapper
public interface PayrollItemMapper extends BaseMapper<PayrollItem> {

    /**
     * 根据工资表ID查询所有工资明细
     */
    @Select("SELECT * FROM hr_payroll_item WHERE payroll_sheet_id = #{payrollSheetId} AND deleted = false ORDER BY employee_no")
    List<PayrollItem> selectByPayrollSheetId(@Param("payrollSheetId") Long payrollSheetId);

    /**
     * 根据员工ID和年月查询工资明细
     */
    @Select("SELECT pi.* FROM hr_payroll_item pi " +
            "INNER JOIN hr_payroll_sheet ps ON pi.payroll_sheet_id = ps.id " +
            "WHERE pi.employee_id = #{employeeId} " +
            "AND ps.payroll_year = #{year} " +
            "AND ps.payroll_month = #{month} " +
            "AND pi.deleted = false " +
            "AND ps.deleted = false " +
            "LIMIT 1")
    PayrollItem selectByEmployeeIdAndYearMonth(@Param("employeeId") Long employeeId,
                                                @Param("year") Integer year,
                                                @Param("month") Integer month);

    /**
     * 统计工资表中已确认的人数
     */
    @Select("SELECT COUNT(*) FROM hr_payroll_item WHERE payroll_sheet_id = #{payrollSheetId} AND confirm_status = 'CONFIRMED' AND deleted = false")
    Integer countConfirmedByPayrollSheetId(@Param("payrollSheetId") Long payrollSheetId);
}

