package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lawfirm.domain.hr.entity.PromotionApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 晋升申请 Mapper
 */
@Mapper
public interface PromotionApplicationMapper extends BaseMapper<PromotionApplication> {

    /**
     * 分页查询晋升申请
     */
    @Select("""
        <script>
        SELECT * FROM hr_promotion_application
        WHERE deleted = false
        <if test="keyword != null and keyword != ''">
            AND (employee_name LIKE CONCAT('%', #{keyword}, '%') OR application_no LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null and status != ''">
            AND status = #{status}
        </if>
        <if test="employeeId != null">
            AND employee_id = #{employeeId}
        </if>
        <if test="departmentId != null">
            AND department_id = #{departmentId}
        </if>
        ORDER BY created_at DESC
        </script>
        """)
    IPage<PromotionApplication> selectApplicationPage(Page<PromotionApplication> page,
                                                       @Param("keyword") String keyword,
                                                       @Param("status") String status,
                                                       @Param("employeeId") Long employeeId,
                                                       @Param("departmentId") Long departmentId);

    /**
     * 根据申请编号查询
     */
    @Select("SELECT * FROM hr_promotion_application WHERE application_no = #{applicationNo} AND deleted = false LIMIT 1")
    PromotionApplication selectByApplicationNo(@Param("applicationNo") String applicationNo);

    /**
     * 统计待审批数量
     */
    @Select("SELECT COUNT(*) FROM hr_promotion_application WHERE deleted = false AND status IN ('PENDING', 'REVIEWING')")
    int countPending();

    /**
     * 更新状态
     */
    @Update("UPDATE hr_promotion_application SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 审批通过
     */
    @Update("""
        UPDATE hr_promotion_application SET 
            status = 'APPROVED',
            approved_by = #{approvedBy},
            approved_by_name = #{approvedByName},
            approved_at = NOW(),
            approval_comment = #{comment},
            effective_date = #{effectiveDate},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int approve(@Param("id") Long id, 
                @Param("approvedBy") Long approvedBy, 
                @Param("approvedByName") String approvedByName,
                @Param("comment") String comment,
                @Param("effectiveDate") LocalDate effectiveDate);

    /**
     * 审批拒绝
     */
    @Update("""
        UPDATE hr_promotion_application SET 
            status = 'REJECTED',
            approved_by = #{approvedBy},
            approved_by_name = #{approvedByName},
            approved_at = NOW(),
            approval_comment = #{comment},
            updated_at = NOW()
        WHERE id = #{id}
        """)
    int reject(@Param("id") Long id, 
               @Param("approvedBy") Long approvedBy, 
               @Param("approvedByName") String approvedByName,
               @Param("comment") String comment);
}
