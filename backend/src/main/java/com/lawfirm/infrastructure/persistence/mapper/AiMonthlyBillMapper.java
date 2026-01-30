package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.domain.ai.entity.AiMonthlyBill;
import com.lawfirm.domain.ai.repository.AiMonthlyBillRepository;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** AI月度账单Mapper */
@Mapper
public interface AiMonthlyBillMapper extends AiMonthlyBillRepository {

  /**
   * 保存AI月度账单.
   *
   * @param bill AI月度账单实体
   */
  @Override
  @Insert(
      """
        INSERT INTO ai_monthly_bill (
            bill_year, bill_month, user_id, user_name, department_id, department_name,
            total_calls, total_tokens, prompt_tokens, completion_tokens,
            total_cost, user_cost, charge_ratio, deduction_status,
            created_at, created_by
        ) VALUES (
            #{billYear}, #{billMonth}, #{userId}, #{userName}, #{departmentId}, #{departmentName},
            #{totalCalls}, #{totalTokens}, #{promptTokens}, #{completionTokens},
            #{totalCost}, #{userCost}, #{chargeRatio}, #{deductionStatus},
            CURRENT_TIMESTAMP, #{createdBy}
        )
        """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void save(AiMonthlyBill bill);

  /**
   * 更新AI月度账单.
   *
   * @param bill AI月度账单实体
   */
  @Override
  @Update(
      """
        UPDATE ai_monthly_bill SET
            total_calls = #{totalCalls},
            total_tokens = #{totalTokens},
            prompt_tokens = #{promptTokens},
            completion_tokens = #{completionTokens},
            total_cost = #{totalCost},
            user_cost = #{userCost},
            charge_ratio = #{chargeRatio},
            deduction_status = #{deductionStatus},
            deduction_amount = #{deductionAmount},
            deducted_at = #{deductedAt},
            deducted_by = #{deductedBy},
            deduction_remark = #{deductionRemark},
            payroll_deduction_id = #{payrollDeductionId},
            updated_at = CURRENT_TIMESTAMP,
            updated_by = #{updatedBy}
        WHERE id = #{id} AND deleted = FALSE
        """)
  void update(AiMonthlyBill bill);

  /**
   * 根据ID查询AI月度账单.
   *
   * @param id 账单ID
   * @return AI月度账单实体
   */
  @Override
  @Select("SELECT * FROM ai_monthly_bill WHERE id = #{id} AND deleted = FALSE")
  AiMonthlyBill findById(Long id);

  /**
   * 根据用户和月份查询AI月度账单.
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return AI月度账单实体
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_monthly_bill
        WHERE user_id = #{userId} AND bill_year = #{year} AND bill_month = #{month}
        AND deleted = FALSE
        """)
  AiMonthlyBill findByUserAndMonth(
      @Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

  /**
   * 根据用户ID查询AI月度账单列表.
   *
   * @param userId 用户ID
   * @return AI月度账单列表
   */
  @Override
  @Select(
      "SELECT * FROM ai_monthly_bill WHERE user_id = #{userId} "
          + "AND deleted = FALSE ORDER BY bill_year DESC, bill_month DESC")
  List<AiMonthlyBill> findByUserId(Long userId);

  /**
   * 根据月份查询AI月度账单列表.
   *
   * @param year 年份
   * @param month 月份
   * @return AI月度账单列表
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_monthly_bill
        WHERE bill_year = #{year} AND bill_month = #{month} AND deleted = FALSE
        ORDER BY user_cost DESC
        """)
  List<AiMonthlyBill> findByMonth(@Param("year") int year, @Param("month") int month);

  /**
   * 根据月份查询待扣款的AI月度账单列表.
   *
   * @param year 年份
   * @param month 月份
   * @return 待扣款的AI月度账单列表
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_monthly_bill
        WHERE bill_year = #{year} AND bill_month = #{month}
        AND deduction_status = 'PENDING' AND deleted = FALSE
        ORDER BY user_cost DESC
        """)
  List<AiMonthlyBill> findPendingByMonth(@Param("year") int year, @Param("month") int month);

  /**
   * 根据扣款状态查询AI月度账单列表.
   *
   * @param status 扣款状态
   * @return AI月度账单列表
   */
  @Override
  @Select(
      "SELECT * FROM ai_monthly_bill WHERE deduction_status = #{status} "
          + "AND deleted = FALSE ORDER BY bill_year DESC, bill_month DESC")
  List<AiMonthlyBill> findByStatus(String status);

  /**
   * 批量更新扣款状态.
   *
   * @param ids 账单ID列表
   * @param status 扣款状态
   * @param operatorId 操作人ID
   * @param remark 备注
   */
  @Override
  @Update(
      """
        <script>
        UPDATE ai_monthly_bill SET
            deduction_status = #{status},
            deducted_at = CURRENT_TIMESTAMP,
            deducted_by = #{operatorId},
            deduction_remark = #{remark},
            updated_at = CURRENT_TIMESTAMP
        WHERE id IN
        <foreach collection="ids" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
        AND deleted = FALSE
        </script>
        """)
  void batchUpdateStatus(
      @Param("ids") List<Long> ids,
      @Param("status") String status,
      @Param("operatorId") Long operatorId,
      @Param("remark") String remark);

  /**
   * 检查用户在指定月份是否存在账单.
   *
   * @param userId 用户ID
   * @param year 年份
   * @param month 月份
   * @return 是否存在
   */
  @Override
  @Select(
      """
        SELECT EXISTS(
            SELECT 1 FROM ai_monthly_bill
            WHERE user_id = #{userId} AND bill_year = #{year} AND bill_month = #{month}
            AND deleted = FALSE
        )
        """)
  boolean existsByUserAndMonth(
      @Param("userId") Long userId, @Param("year") int year, @Param("month") int month);

  /**
   * 根据ID删除AI月度账单（逻辑删除）.
   *
   * @param id 账单ID
   */
  @Override
  @Update(
      "UPDATE ai_monthly_bill SET deleted = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  void deleteById(Long id);
}
