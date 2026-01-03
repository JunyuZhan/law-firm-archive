package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工档案 Mapper
 */
@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 分页查询员工档案
     */
    @Select("""
        <script>
        SELECT e.*, u.real_name, u.email, u.phone, u.department_id, d.name as department_name
        FROM hr_employee e
        LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = false
        LEFT JOIN sys_department d ON u.department_id = d.id AND d.deleted = false
        WHERE e.deleted = false
        <if test="employeeNo != null and employeeNo != ''">
            AND e.employee_no LIKE CONCAT('%', #{employeeNo}, '%')
        </if>
        <if test="realName != null and realName != ''">
            AND u.real_name LIKE CONCAT('%', #{realName}, '%')
        </if>
        <if test="departmentId != null">
            AND u.department_id = #{departmentId}
        </if>
        <if test="workStatus != null and workStatus != ''">
            AND e.work_status = #{workStatus}
        </if>
        <if test="position != null and position != ''">
            AND e.position LIKE CONCAT('%', #{position}, '%')
        </if>
        ORDER BY e.id DESC
        </script>
        """)
    IPage<Employee> selectEmployeePage(Page<Employee> page,
                                       @Param("employeeNo") String employeeNo,
                                       @Param("realName") String realName,
                                       @Param("departmentId") Long departmentId,
                                       @Param("workStatus") String workStatus,
                                       @Param("position") String position);

    /**
     * 根据用户ID查询员工档案
     */
    @Select("SELECT * FROM hr_employee WHERE user_id = #{userId} AND deleted = false LIMIT 1")
    Employee selectByUserId(@Param("userId") Long userId);
}

