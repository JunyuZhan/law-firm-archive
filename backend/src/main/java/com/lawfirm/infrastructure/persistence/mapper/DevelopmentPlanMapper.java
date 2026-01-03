package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.DevelopmentPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 个人发展规划 Mapper
 */
@Mapper
public interface DevelopmentPlanMapper extends BaseMapper<DevelopmentPlan> {

    /**
     * 分页查询发展规划
     */
    @Select("""
        <script>
        SELECT * FROM hr_development_plan
        WHERE deleted = false
        <if test="keyword != null and keyword != ''">
            AND (employee_name LIKE CONCAT('%', #{keyword}, '%') OR plan_title LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        <if test="employeeId != null">
            AND employee_id = #{employeeId}
        </if>
        <if test="planYear != null">
            AND plan_year = #{planYear}
        </if>
        ORDER BY created_at DESC
        </script>
        """)
    IPage<DevelopmentPlan> selectPlanPage(Page<DevelopmentPlan> page,
                                           @Param("keyword") String keyword,
                                           @Param("status") String status,
                                           @Param("employeeId") Long employeeId,
                                           @Param("planYear") Integer planYear);

    /**
     * 根据规划编号查询
     */
    @Select("SELECT * FROM hr_development_plan WHERE plan_no = #{planNo} AND deleted = false LIMIT 1")
    DevelopmentPlan selectByPlanNo(@Param("planNo") String planNo);

    /**
     * 查询员工当年规划
     */
    @Select("SELECT * FROM hr_development_plan WHERE employee_id = #{employeeId} AND plan_year = #{planYear} AND deleted = false LIMIT 1")
    DevelopmentPlan selectByEmployeeAndYear(@Param("employeeId") Long employeeId, @Param("planYear") Integer planYear);
}
