package com.lawfirm.infrastructure.persistence.mapper;

import com.lawfirm.application.ai.dto.AiUsageLogDTO;
import com.lawfirm.application.ai.dto.AiUsageQueryDTO;
import com.lawfirm.common.result.PageResult;
import com.lawfirm.domain.ai.entity.AiUsageLog;
import com.lawfirm.domain.ai.repository.AiUsageLogRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** AI使用记录Mapper */
@Mapper
public interface AiUsageLogMapper extends AiUsageLogRepository {

  // ==================== queryPage 和 getSummary 需要在 default 方法中实现 ====================

  /**
   * 分页查询（default方法实现）.
   *
   * @param query 查询条件
   * @return 分页结果
   */
  @Override
  default PageResult<AiUsageLogDTO> queryPage(AiUsageQueryDTO query) {
    int offset = (query.getPageNum() - 1) * query.getPageSize();
    List<AiUsageLogDTO> list = selectPageList(query, offset, query.getPageSize());
    long total = selectPageCount(query);
    return PageResult.of(list, total, query.getPageNum(), query.getPageSize());
  }

  /**
   * 获取统计摘要（调用已有方法）.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 统计摘要Map
   */
  @Override
  default Map<String, Object> getSummary(Long userId, LocalDate startDate, LocalDate endDate) {
    return getUserMonthlySummary(userId, startDate, endDate);
  }

  /**
   * 分页查询列表.
   *
   * @param query 查询条件
   * @param offset 偏移量
   * @param limit 限制数量
   * @return AI使用记录DTO列表
   */
  @Select(
      """
        <script>
        SELECT id, user_id, user_name, department_id, department_name,
               integration_code, integration_name, model_name,
               request_type, business_type, business_id,
               prompt_tokens, completion_tokens, total_tokens,
               total_cost, user_cost, charge_ratio,
               success, error_message, duration_ms, created_at
        FROM ai_usage_log
        WHERE 1=1
        <if test="query.userId != null"> AND user_id = #{query.userId} </if>
        <if test="query.integrationCode != null"> AND integration_code = #{query.integrationCode} </if>
        <if test="query.modelName != null"> AND model_name = #{query.modelName} </if>
        <if test="query.requestType != null"> AND request_type = #{query.requestType} </if>
        <if test="query.businessType != null"> AND business_type = #{query.businessType} </if>
        <if test="query.success != null"> AND success = #{query.success} </if>
        <if test="query.createdAtFrom != null"> AND created_at &gt;= #{query.createdAtFrom} </if>
        <if test="query.createdAtTo != null"> AND created_at &lt;= #{query.createdAtTo} </if>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
  List<AiUsageLogDTO> selectPageList(
      @Param("query") AiUsageQueryDTO query,
      @Param("offset") int offset,
      @Param("limit") int limit);

  /**
   * 分页查询总数.
   *
   * @param query 查询条件
   * @return 总数
   */
  @Select(
      """
        <script>
        SELECT COUNT(*) FROM ai_usage_log
        WHERE 1=1
        <if test="query.userId != null"> AND user_id = #{query.userId} </if>
        <if test="query.integrationCode != null"> AND integration_code = #{query.integrationCode} </if>
        <if test="query.modelName != null"> AND model_name = #{query.modelName} </if>
        <if test="query.requestType != null"> AND request_type = #{query.requestType} </if>
        <if test="query.businessType != null"> AND business_type = #{query.businessType} </if>
        <if test="query.success != null"> AND success = #{query.success} </if>
        <if test="query.createdAtFrom != null"> AND created_at &gt;= #{query.createdAtFrom} </if>
        <if test="query.createdAtTo != null"> AND created_at &lt;= #{query.createdAtTo} </if>
        </script>
        """)
  long selectPageCount(@Param("query") AiUsageQueryDTO query);

  /**
   * 保存AI使用记录.
   *
   * @param usageLog AI使用记录实体
   */
  @Override
  @Insert(
      """
        INSERT INTO ai_usage_log (
            user_id, user_name, department_id, department_name,
            integration_id, integration_code, integration_name, model_name,
            request_type, business_type, business_id,
            prompt_tokens, completion_tokens, total_tokens,
            prompt_price, completion_price, total_cost, user_cost, charge_ratio,
            success, error_message, duration_ms, created_at
        ) VALUES (
            #{userId}, #{userName}, #{departmentId}, #{departmentName},
            #{integrationId}, #{integrationCode}, #{integrationName}, #{modelName},
            #{requestType}, #{businessType}, #{businessId},
            #{promptTokens}, #{completionTokens}, #{totalTokens},
            #{promptPrice}, #{completionPrice}, #{totalCost}, #{userCost}, #{chargeRatio},
            #{success}, #{errorMessage}, #{durationMs}, CURRENT_TIMESTAMP
        )
        """)
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void save(AiUsageLog usageLog);

  /**
   * 根据ID查询AI使用记录.
   *
   * @param id 记录ID
   * @return AI使用记录实体
   */
  @Override
  @Select("SELECT * FROM ai_usage_log WHERE id = #{id}")
  AiUsageLog findById(Long id);

  /**
   * 根据用户ID分页查询AI使用记录.
   *
   * @param userId 用户ID
   * @param offset 偏移量
   * @param limit 限制数量
   * @return AI使用记录列表
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_usage_log
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
  List<AiUsageLog> findByUserId(
      @Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

  /**
   * 根据用户ID统计记录数.
   *
   * @param userId 用户ID
   * @return 记录数
   */
  @Override
  @Select("SELECT COUNT(*) FROM ai_usage_log WHERE user_id = #{userId}")
  long countByUserId(Long userId);

  /**
   * 根据用户ID和时间范围查询AI使用记录.
   *
   * @param userId 用户ID
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @return AI使用记录列表
   */
  @Override
  @Select(
      """
        SELECT * FROM ai_usage_log
        WHERE user_id = #{userId}
        AND created_at >= #{startTime} AND created_at < #{endTime}
        ORDER BY created_at DESC
        """)
  List<AiUsageLog> findByUserIdAndTimeRange(
      @Param("userId") Long userId,
      @Param("startTime") LocalDateTime startTime,
      @Param("endTime") LocalDateTime endTime);

  /**
   * 获取用户月度使用摘要.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 使用摘要Map
   */
  @Override
  @Select(
      """
        SELECT
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(prompt_tokens), 0) as prompt_tokens,
            COALESCE(SUM(completion_tokens), 0) as completion_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log
        WHERE user_id = #{userId}
        AND created_at >= #{startDate}::timestamp
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        """)
  Map<String, Object> getUserMonthlySummary(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 按模型统计使用情况.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 按模型统计的使用情况列表
   */
  @Override
  @Select(
      """
        SELECT
            integration_code,
            integration_name,
            model_name,
            COUNT(*) as call_count,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log
        WHERE user_id = #{userId}
        AND created_at >= #{startDate}::timestamp
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        GROUP BY integration_code, integration_name, model_name
        ORDER BY total_cost DESC
        """)
  List<Map<String, Object>> getUsageByModel(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 获取所有用户的月度使用摘要.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 所有用户的月度使用摘要列表
   */
  @Override
  @Select(
      """
        SELECT
            user_id,
            user_name,
            department_id,
            department_name,
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(prompt_tokens), 0) as prompt_tokens,
            COALESCE(SUM(completion_tokens), 0) as completion_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log
        WHERE created_at >= #{startDate}::timestamp
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        GROUP BY user_id, user_name, department_id, department_name
        ORDER BY total_cost DESC
        """)
  List<Map<String, Object>> getAllUsersMonthlySummary(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  /**
   * 获取部门使用摘要.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 部门使用摘要列表
   */
  @Override
  @Select(
      """
        SELECT
            department_id,
            department_name,
            COUNT(DISTINCT user_id) as user_count,
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log
        WHERE created_at >= #{startDate}::timestamp
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        AND department_id IS NOT NULL
        GROUP BY department_id, department_name
        ORDER BY total_cost DESC
        """)
  List<Map<String, Object>> getDepartmentSummary(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

  /**
   * 获取需要计费的用户列表.
   *
   * @param year 年份
   * @param month 月份
   * @return 需要计费的用户列表
   */
  @Override
  @Select(
      """
        SELECT
            user_id,
            user_name,
            department_id,
            department_name,
            COUNT(*) as total_calls,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(prompt_tokens), 0) as prompt_tokens,
            COALESCE(SUM(completion_tokens), 0) as completion_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost,
            MAX(charge_ratio) as charge_ratio
        FROM ai_usage_log
        WHERE EXTRACT(YEAR FROM created_at) = #{year}
        AND EXTRACT(MONTH FROM created_at) = #{month}
        GROUP BY user_id, user_name, department_id, department_name
        HAVING SUM(user_cost) > 0
        ORDER BY user_cost DESC
        """)
  List<Map<String, Object>> getUsersForBilling(@Param("year") int year, @Param("month") int month);

  // ==================== 新增方法 ====================

  /**
   * 获取使用趋势.
   *
   * @param userId 用户ID
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 使用趋势列表
   */
  @Override
  @Select(
      """
        SELECT
            DATE(created_at) as date,
            COUNT(*) as call_count,
            COALESCE(SUM(total_tokens), 0) as total_tokens,
            COALESCE(SUM(total_cost), 0) as total_cost,
            COALESCE(SUM(user_cost), 0) as user_cost
        FROM ai_usage_log
        WHERE user_id = #{userId}
        AND created_at >= #{startDate}::timestamp
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        GROUP BY DATE(created_at)
        ORDER BY date
        """)
  List<Map<String, Object>> getUsageTrend(
      @Param("userId") Long userId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * 获取所有用户摘要（default方法实现）.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 所有用户摘要DTO列表
   */
  @Override
  default List<com.lawfirm.application.ai.dto.AiUsageSummaryDTO> getAllUsersSummary(
      LocalDate startDate, LocalDate endDate) {
    List<Map<String, Object>> rawData = getAllUsersMonthlySummary(startDate, endDate);
    return rawData.stream()
        .map(
            row ->
                com.lawfirm.application.ai.dto.AiUsageSummaryDTO.builder()
                    .userId(
                        row.get("user_id") != null
                            ? ((Number) row.get("user_id")).longValue()
                            : null)
                    .userName((String) row.get("user_name"))
                    .departmentId(
                        row.get("department_id") != null
                            ? ((Number) row.get("department_id")).longValue()
                            : null)
                    .departmentName((String) row.get("department_name"))
                    .totalCalls(
                        row.get("total_calls") != null
                            ? ((Number) row.get("total_calls")).intValue()
                            : 0)
                    .totalTokens(
                        row.get("total_tokens") != null
                            ? ((Number) row.get("total_tokens")).longValue()
                            : 0L)
                    .promptTokens(
                        row.get("prompt_tokens") != null
                            ? ((Number) row.get("prompt_tokens")).longValue()
                            : 0L)
                    .completionTokens(
                        row.get("completion_tokens") != null
                            ? ((Number) row.get("completion_tokens")).longValue()
                            : 0L)
                    .totalCost(
                        row.get("total_cost") != null
                            ? new java.math.BigDecimal(row.get("total_cost").toString())
                            : java.math.BigDecimal.ZERO)
                    .userCost(
                        row.get("user_cost") != null
                            ? new java.math.BigDecimal(row.get("user_cost").toString())
                            : java.math.BigDecimal.ZERO)
                    .build())
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * 获取指定时间段内的活跃用户.
   *
   * @param startDate 开始日期
   * @param endDate 结束日期
   * @return 活跃用户列表
   */
  @Override
  @Select(
      """
        SELECT DISTINCT user_id, user_name
        FROM ai_usage_log
        WHERE created_at >= #{startDate}::timestamp
        AND created_at < (#{endDate}::date + INTERVAL '1 day')::timestamp
        """)
  List<Map<String, Object>> getActiveUsersInPeriod(
      @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
