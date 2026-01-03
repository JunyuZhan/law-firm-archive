package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Commission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 提成记录 Mapper
 */
@Mapper
public interface CommissionMapper extends BaseMapper<Commission> {

    /**
     * 根据收款ID查询提成记录
     */
    @Select("SELECT * FROM finance_commission WHERE payment_id = #{paymentId} AND deleted = false ORDER BY id")
    List<Commission> selectByPaymentId(@Param("paymentId") Long paymentId);

    /**
     * 根据案件ID查询提成记录
     */
    @Select("SELECT * FROM finance_commission WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
    List<Commission> selectByMatterId(@Param("matterId") Long matterId);

    /**
     * 根据用户ID查询提成记录
     */
    @Select("""
        SELECT * FROM finance_commission
        WHERE user_id = #{userId} AND deleted = false
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<Commission> selectByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 统计用户提成总额
     */
    @Select("""
        SELECT COALESCE(SUM(commission_amount), 0) FROM finance_commission
        WHERE user_id = #{userId} AND status = 'PAID' AND deleted = false
        """)
    java.math.BigDecimal sumCommissionByUserId(@Param("userId") Long userId);

    /**
     * 统计总提成金额
     */
    @Select("""
        <script>
        SELECT COALESCE(SUM(commission_amount), 0) FROM finance_commission
        WHERE deleted = false
        <if test="startDate != null and startDate != ''">
            AND created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND created_at &lt;= #{endDate}::date
        </if>
        </script>
        """)
    java.math.BigDecimal sumTotalCommission(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 按状态统计提成金额
     */
    @Select("""
        <script>
        SELECT COALESCE(SUM(commission_amount), 0) FROM finance_commission
        WHERE status = #{status} AND deleted = false
        <if test="startDate != null and startDate != ''">
            AND created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND created_at &lt;= #{endDate}::date
        </if>
        </script>
        """)
    java.math.BigDecimal sumCommissionByStatus(@Param("status") String status, 
                                               @Param("startDate") String startDate, 
                                               @Param("endDate") String endDate);

    /**
     * 统计提成记录数
     */
    @Select("""
        <script>
        SELECT COUNT(*) FROM finance_commission
        WHERE deleted = false
        <if test="startDate != null and startDate != ''">
            AND created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND created_at &lt;= #{endDate}::date
        </if>
        </script>
        """)
    Long countCommissions(@Param("startDate") String startDate, @Param("endDate") String endDate);

    /**
     * 按用户汇总提成
     */
    @Select("""
        <script>
        SELECT 
            user_id,
            u.real_name as user_name,
            COUNT(*) as commission_count,
            COALESCE(SUM(commission_amount), 0) as total_commission,
            COALESCE(SUM(CASE WHEN status = 'PAID' THEN commission_amount ELSE 0 END), 0) as paid_commission
        FROM finance_commission c
        LEFT JOIN sys_user u ON c.user_id = u.id
        WHERE c.deleted = false
        <if test="startDate != null and startDate != ''">
            AND c.created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND c.created_at &lt;= #{endDate}::date
        </if>
        GROUP BY user_id, u.real_name
        ORDER BY total_commission DESC
        </script>
        """)
    List<java.util.Map<String, Object>> sumCommissionByUser(@Param("startDate") String startDate, 
                                                              @Param("endDate") String endDate);

    /**
     * 查询提成报表数据
     */
    @Select("""
        <script>
        SELECT 
            c.id,
            c.matter_id,
            m.name as matter_name,
            c.user_id,
            u.real_name as user_name,
            c.commission_amount,
            c.status,
            c.created_at,
            c.approved_at,
            c.paid_at
        FROM finance_commission c
        LEFT JOIN matter m ON c.matter_id = m.id
        LEFT JOIN sys_user u ON c.user_id = u.id
        WHERE c.deleted = false
        <if test="startDate != null and startDate != ''">
            AND c.created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND c.created_at &lt;= #{endDate}::date
        </if>
        <if test="userId != null">
            AND c.user_id = #{userId}
        </if>
        ORDER BY c.created_at DESC
        </script>
        """)
    List<java.util.Map<String, Object>> queryCommissionReportData(@Param("startDate") String startDate,
                                                                    @Param("endDate") String endDate,
                                                                    @Param("userId") Long userId);
}

