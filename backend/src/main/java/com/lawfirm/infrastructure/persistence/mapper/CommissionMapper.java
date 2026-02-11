package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Commission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 提成记录 Mapper */
@Mapper
public interface CommissionMapper extends BaseMapper<Commission> {

  /**
   * 根据收款ID查询提成记录.
   *
   * @param paymentId 收款ID
   * @return 提成记录列表
   */
  @Select(
      "SELECT * FROM finance_commission WHERE payment_id = #{paymentId} AND deleted = false ORDER BY id")
  List<Commission> selectByPaymentId(@Param("paymentId") Long paymentId);

  /**
   * 根据案件ID查询提成记录.
   *
   * @param matterId 案件ID
   * @return 提成记录列表
   */
  @Select(
      "SELECT * FROM finance_commission WHERE matter_id = #{matterId} AND deleted = false ORDER BY created_at DESC")
  List<Commission> selectByMatterId(@Param("matterId") Long matterId);

  /**
   * 根据用户ID查询提成记录（通过 commission_detail 表关联）.
   *
   * @param userId 用户ID
   * @param offset 偏移量
   * @param limit 限制数量
   * @return 提成记录列表
   */
  @Select(
      """
        SELECT DISTINCT c.* FROM finance_commission c
        INNER JOIN finance_commission_detail cd ON c.id = cd.commission_id
        WHERE cd.user_id = #{userId} AND c.deleted = false AND cd.deleted = false
        ORDER BY c.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
  List<Commission> selectByUserId(
      @Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

  /**
   * 根据用户ID查询所有提成记录（不分页，通过 commission_detail 表关联）.
   *
   * @param userId 用户ID
   * @return 提成记录列表
   */
  @Select(
      """
        SELECT DISTINCT c.* FROM finance_commission c
        INNER JOIN finance_commission_detail cd ON c.id = cd.commission_id
        WHERE cd.user_id = #{userId} AND c.deleted = false AND cd.deleted = false
        ORDER BY c.created_at DESC
        """)
  List<Commission> selectAllByUserId(@Param("userId") Long userId);

  /**
   * 根据用户ID和状态查询提成记录（通过 commission_detail 表关联）.
   *
   * @param userId 用户ID
   * @param status 状态
   * @return 提成记录列表
   */
  @Select(
      """
        SELECT DISTINCT c.* FROM finance_commission c
        INNER JOIN finance_commission_detail cd ON c.id = cd.commission_id
        WHERE cd.user_id = #{userId} AND c.status = #{status} AND c.deleted = false AND cd.deleted = false
        ORDER BY c.created_at DESC
        """)
  List<Commission> selectByUserIdAndStatus(
      @Param("userId") Long userId, @Param("status") String status);

  /**
   * 统计用户提成总额（通过 commission_detail 表关联）.
   *
   * @param userId 用户ID
   * @return 提成总额
   */
  @Select(
      """
        SELECT COALESCE(SUM(cd.commission_amount), 0) FROM finance_commission_detail cd
        INNER JOIN finance_commission c ON cd.commission_id = c.id
        WHERE cd.user_id = #{userId} AND c.status = 'PAID' AND c.deleted = false AND cd.deleted = false
        """)
  java.math.BigDecimal sumCommissionByUserId(@Param("userId") Long userId);

  /**
   * 统计总提成金额.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 总提成金额
   */
  @Select(
      """
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
  java.math.BigDecimal sumTotalCommission(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /**
   * 按状态统计提成金额.
   *
   * @param status 状态
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 提成金额
   */
  @Select(
      """
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
  java.math.BigDecimal sumCommissionByStatus(
      @Param("status") String status,
      @Param("startDate") String startDate,
      @Param("endDate") String endDate);

  /**
   * 统计提成记录数.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 记录数
   */
  @Select(
      """
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
   * 按用户汇总提成（通过 commission_detail 表关联）.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 汇总结果
   */
  @Select(
      """
        <script>
        SELECT
            cd.user_id,
            u.real_name as user_name,
            COUNT(DISTINCT c.id) as commission_count,
            COALESCE(SUM(cd.commission_amount), 0) as total_commission,
            COALESCE(SUM(CASE WHEN c.status = 'PAID' THEN cd.commission_amount ELSE 0 END), 0) as paid_commission
        FROM finance_commission_detail cd
        INNER JOIN finance_commission c ON cd.commission_id = c.id
        LEFT JOIN sys_user u ON cd.user_id = u.id
        WHERE c.deleted = false AND cd.deleted = false
        <if test="startDate != null and startDate != ''">
            AND c.created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND c.created_at &lt;= #{endDate}::date
        </if>
        GROUP BY cd.user_id, u.real_name
        ORDER BY total_commission DESC
        </script>
        """)
  List<java.util.Map<String, Object>> sumCommissionByUser(
      @Param("startDate") String startDate, @Param("endDate") String endDate);

  /**
   * 查询提成报表数据（通过 commission_detail 表关联）.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @param userId 用户ID
   * @return 报表数据
   */
  @Select(
      """
        <script>
        SELECT
            c.id,
            c.matter_id,
            m.name as matter_name,
            cd.user_id,
            u.real_name as user_name,
            cd.commission_amount,
            c.status,
            c.created_at,
            c.approved_at,
            c.paid_at
        FROM finance_commission_detail cd
        INNER JOIN finance_commission c ON cd.commission_id = c.id
        LEFT JOIN matter m ON c.matter_id = m.id
        LEFT JOIN sys_user u ON cd.user_id = u.id
        WHERE c.deleted = false AND cd.deleted = false
        <if test="startDate != null and startDate != ''">
            AND c.created_at >= #{startDate}::date
        </if>
        <if test="endDate != null and endDate != ''">
            AND c.created_at &lt;= #{endDate}::date
        </if>
        <if test="userId != null">
            AND cd.user_id = #{userId}
        </if>
        ORDER BY c.created_at DESC
        </script>
        """)
  List<java.util.Map<String, Object>> queryCommissionReportData(
      @Param("startDate") String startDate,
      @Param("endDate") String endDate,
      @Param("userId") Long userId);

  /**
   * 检查用户是否有权限查看提成记录（通过 commission_detail 表检查）.
   *
   * @param commissionId 提成ID
   * @param userId 用户ID
   * @return 记录数
   */
  @Select(
      """
        SELECT COUNT(*) FROM finance_commission_detail cd
        WHERE cd.commission_id = #{commissionId}
        AND cd.user_id = #{userId}
        AND cd.deleted = false
        """)
  int countByCommissionIdAndUserId(
      @Param("commissionId") Long commissionId, @Param("userId") Long userId);

  /**
   * ✅ 修复问题553: 批量查询多个用户的提成总额（避免N+1查询）.
   *
   * @param userIds 用户ID列表
   * @return 汇总结果
   */
  @Select(
      """
        <script>
        SELECT
            cd.user_id,
            COALESCE(SUM(cd.commission_amount), 0) as total_commission
        FROM finance_commission_detail cd
        INNER JOIN finance_commission c ON cd.commission_id = c.id
        WHERE c.status = 'PAID'
        AND c.deleted = false
        AND cd.deleted = false
        AND cd.user_id IN
        <foreach collection="userIds" item="userId" open="(" separator="," close=")">
            #{userId}
        </foreach>
        GROUP BY cd.user_id
        </script>
        """)
  List<java.util.Map<String, Object>> sumCommissionGroupByUserId(
      @Param("userIds") List<Long> userIds);

  /**
   * 批量根据多个用户ID查询所有提成记录（避免N+1查询）.
   *
   * @param userIds 用户ID列表
   * @return 提成记录列表（包含user_id字段用于分组）
   */
  @Select(
      """
        <script>
        SELECT DISTINCT c.*, cd.user_id as detail_user_id FROM finance_commission c
        INNER JOIN finance_commission_detail cd ON c.id = cd.commission_id
        WHERE c.deleted = false AND cd.deleted = false
        AND cd.user_id IN
        <foreach collection="userIds" item="userId" open="(" separator="," close=")">
            #{userId}
        </foreach>
        ORDER BY c.created_at DESC
        </script>
        """)
  List<java.util.Map<String, Object>> selectAllByUserIds(@Param("userIds") List<Long> userIds);
}
