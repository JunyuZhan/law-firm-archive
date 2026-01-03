package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.Contract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 劳动合同 Mapper
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    /**
     * 分页查询劳动合同
     */
    @Select("""
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
    IPage<Contract> selectContractPage(Page<Contract> page,
                                        @Param("employeeId") Long employeeId,
                                        @Param("contractNo") String contractNo,
                                        @Param("status") String status);

    /**
     * 根据员工ID查询生效中的合同
     */
    @Select("SELECT * FROM hr_contract WHERE employee_id = #{employeeId} AND status = 'ACTIVE' AND deleted = false LIMIT 1")
    Contract selectActiveContractByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 根据员工ID查询所有合同
     */
    @Select("SELECT * FROM hr_contract WHERE employee_id = #{employeeId} AND deleted = false ORDER BY start_date DESC")
    List<Contract> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
