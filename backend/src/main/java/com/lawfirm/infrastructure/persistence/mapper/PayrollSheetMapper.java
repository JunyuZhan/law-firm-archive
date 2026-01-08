package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.PayrollSheet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工资表 Mapper
 */
@Mapper
public interface PayrollSheetMapper extends BaseMapper<PayrollSheet> {

    /**
     * 分页查询工资表
     */
    @Select("<script>" +
            "SELECT * FROM hr_payroll_sheet " +
            "WHERE deleted = false " +
            "<if test=\"payrollYear != null\">" +
            "AND payroll_year = #{payrollYear}" +
            "</if>" +
            "<if test=\"payrollMonth != null\">" +
            "AND payroll_month = #{payrollMonth}" +
            "</if>" +
            "<if test=\"status != null and status != ''\">" +
            "AND status = #{status}" +
            "</if>" +
            "<if test=\"payrollNo != null and payrollNo != ''\">" +
            "AND payroll_no LIKE CONCAT('%', #{payrollNo}, '%')" +
            "</if>" +
            "ORDER BY payroll_year DESC, payroll_month DESC, created_at DESC" +
            "</script>")
    IPage<PayrollSheet> selectPayrollSheetPage(Page<PayrollSheet> page,
                                                 @Param("payrollYear") Integer payrollYear,
                                                 @Param("payrollMonth") Integer payrollMonth,
                                                 @Param("status") String status,
                                                 @Param("payrollNo") String payrollNo);

    /**
     * 根据年月查询工资表
     */
    @Select("SELECT * FROM hr_payroll_sheet WHERE payroll_year = #{year} AND payroll_month = #{month} AND deleted = false LIMIT 1")
    PayrollSheet selectByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
}

