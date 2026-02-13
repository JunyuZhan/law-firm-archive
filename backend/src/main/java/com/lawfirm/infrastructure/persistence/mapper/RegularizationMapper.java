package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.Regularization;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 转正申请 Mapper */
@Mapper
public interface RegularizationMapper extends BaseMapper<Regularization> {

  /**
   * 分页查询转正申请.
   *
   * @param page 分页参数
   * @param employeeId 员工ID
   * @param status 状态
   * @return 转正申请分页结果
   */
  @Select(
      """
        <script>
        SELECT r.*, e.user_id, u.real_name as employee_name
        FROM hr_regularization r
        LEFT JOIN hr_employee e ON r.employee_id = e.id AND e.deleted = false
        LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = false
        WHERE r.deleted = false
        <if test="employeeId != null">
            AND r.employee_id = #{employeeId}
        </if>
        <if test="status != null and status != ''">
            AND r.status = #{status}
        </if>
        ORDER BY r.application_date DESC
        </script>
        """)
  IPage<Regularization> selectRegularizationPage(
      Page<Regularization> page,
      @Param("employeeId") Long employeeId,
      @Param("status") String status);

  /**
   * 根据员工ID查询转正申请.
   *
   * @param employeeId 员工ID
   * @return 转正申请列表
   */
  @Select(
      "SELECT * FROM hr_regularization WHERE employee_id = #{employeeId} "
          + "AND deleted = false ORDER BY application_date DESC")
  List<Regularization> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
