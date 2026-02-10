package com.lawfirm.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lawfirm.domain.finance.entity.Expense;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 费用报销 Mapper */
@Mapper
public interface ExpenseMapper extends BaseMapper<Expense> {

  /**
   * 分页查询费用报销列表.
   *
   * @param expenseNo 报销单号
   * @param matterId 项目ID
   * @param applicantId 申请人ID
   * @param status 状态
   * @param expenseType 费用类型
   * @param expenseCategory 费用分类
   * @param matterIds 项目ID列表
   * @param offset 偏移量
   * @param limit 每页大小
   * @return 费用报销列表
   */
  @Select(
      """
        <script>
        SELECT e.*, u1.real_name as applicant_name, u2.real_name as approver_name,
               m.name as matter_name
        FROM finance_expense e
        LEFT JOIN sys_user u1 ON e.applicant_id = u1.id
        LEFT JOIN sys_user u2 ON e.approver_id = u2.id
        LEFT JOIN matter m ON e.matter_id = m.id
        WHERE e.deleted = false
        <if test="expenseNo != null and expenseNo != ''">
            AND e.expense_no LIKE CONCAT('%', #{expenseNo}, '%')
        </if>
        <if test="matterId != null">
            AND e.matter_id = #{matterId}
        </if>
        <if test="applicantId != null">
            AND e.applicant_id = #{applicantId}
        </if>
        <if test="status != null and status != ''">
            AND e.status = #{status}
        </if>
        <if test="expenseType != null and expenseType != ''">
            AND e.expense_type = #{expenseType}
        </if>
        <if test="expenseCategory != null and expenseCategory != ''">
            AND e.expense_category = #{expenseCategory}
        </if>
        <if test="matterIds != null and matterIds.size() > 0">
            AND e.matter_id IN
            <foreach collection="matterIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
        ORDER BY e.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
  List<Expense> selectExpensePage(
      @Param("expenseNo") String expenseNo,
      @Param("matterId") Long matterId,
      @Param("applicantId") Long applicantId,
      @Param("status") String status,
      @Param("expenseType") String expenseType,
      @Param("expenseCategory") String expenseCategory,
      @Param("matterIds") java.util.List<Long> matterIds,
      @Param("offset") int offset,
      @Param("limit") int limit);

  /**
   * 统计费用报销总数.
   *
   * @param expenseNo 报销单号
   * @param matterId 项目ID
   * @param applicantId 申请人ID
   * @param status 状态
   * @param expenseType 费用类型
   * @param expenseCategory 费用分类
   * @param matterIds 项目ID列表
   * @return 总数
   */
  @Select(
      """
        <script>
        SELECT COUNT(*)
        FROM finance_expense e
        WHERE e.deleted = false
        <if test="expenseNo != null and expenseNo != ''">
            AND e.expense_no LIKE CONCAT('%', #{expenseNo}, '%')
        </if>
        <if test="matterId != null">
            AND e.matter_id = #{matterId}
        </if>
        <if test="applicantId != null">
            AND e.applicant_id = #{applicantId}
        </if>
        <if test="status != null and status != ''">
            AND e.status = #{status}
        </if>
        <if test="expenseType != null and expenseType != ''">
            AND e.expense_type = #{expenseType}
        </if>
        <if test="expenseCategory != null and expenseCategory != ''">
            AND e.expense_category = #{expenseCategory}
        </if>
        <if test="matterIds != null and matterIds.size() > 0">
            AND e.matter_id IN
            <foreach collection="matterIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </if>
        </script>
        """)
  long countExpensePage(
      @Param("expenseNo") String expenseNo,
      @Param("matterId") Long matterId,
      @Param("applicantId") Long applicantId,
      @Param("status") String status,
      @Param("expenseType") String expenseType,
      @Param("expenseCategory") String expenseCategory,
      @Param("matterIds") java.util.List<Long> matterIds);

  /**
   * 根据报销单号查询.
   *
   * @param expenseNo 报销单号
   * @return 费用报销
   */
  @Select("SELECT * FROM finance_expense WHERE expense_no = #{expenseNo} AND deleted = false")
  Expense selectByExpenseNo(@Param("expenseNo") String expenseNo);

  /**
   * 查询项目的总成本（已归集的费用）.
   *
   * @param matterId 项目ID
   * @return 总成本
   */
  @Select(
      """
        SELECT COALESCE(SUM(amount), 0)
        FROM finance_expense
        WHERE matter_id = #{matterId}
          AND status = 'PAID'
          AND is_cost_allocation = true
          AND deleted = false
        """)
  java.math.BigDecimal selectTotalCostByMatterId(@Param("matterId") Long matterId);
}
