package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.Contract;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 劳动合同 Mapper */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

  /**
   * 分页查询劳动合同.
   *
   * @param page 分页对象
   * @param employeeId 员工ID
   * @param contractNo 合同编号
   * @param status 状态
   * @return 合同分页结果
   */
  @Select(
      """
        <script>
        SELECT c.*, e.user_id, u.real_name as employee_name
        FROM hr_contract c
        LEFT JOIN hr_employee e ON c.employee_id = e.id AND e.deleted = false
        LEFT JOIN sys_user u ON e.user_id = u.id AND u.deleted = false
        WHERE c.deleted = false
        <if test="employeeId != null">
            AND c.employee_id = #{employeeId}
        </if>
        <if test="contractNo != null and contractNo != ''">
            AND c.contract_no LIKE CONCAT('%', #{contractNo}, '%')
        </if>
        <if test="status != null and status != ''">
            AND c.status = #{status}
        </if>
        ORDER BY c.start_date DESC
        </script>
        """)
  IPage<Contract> selectContractPage(
      Page<Contract> page,
      @Param("employeeId") Long employeeId,
      @Param("contractNo") String contractNo,
      @Param("status") String status);

  /**
   * 根据员工ID查询生效中的合同.
   *
   * @param employeeId 员工ID
   * @return 合同信息
   */
  @Select(
      "SELECT * FROM hr_contract WHERE employee_id = #{employeeId} AND status = 'ACTIVE' AND deleted = false LIMIT 1")
  Contract selectActiveContractByEmployeeId(@Param("employeeId") Long employeeId);

  /**
   * 根据员工ID查询所有合同.
   *
   * @param employeeId 员工ID
   * @return 合同列表
   */
  @Select(
      "SELECT * FROM hr_contract WHERE employee_id = #{employeeId} AND deleted = false ORDER BY start_date DESC")
  List<Contract> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
